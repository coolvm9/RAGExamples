package com.fusion.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class YugabyteEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(YugabyteEmbeddingService.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public YugabyteEmbeddingService(
            @Value("${yugabyte.host}") String host,
            @Value("${yugabyte.port}") int port,
            @Value("${yugabyte.database}") String database,
            @Value("${yugabyte.username}") String username,
            @Value("${yugabyte.password}") String password,
            @Value("${yugabyte.table}") String table,
            @Value("${yugabyte.dimension}") int dimension) {

        logger.info("Initializing YugabyteEmbeddingService with host: {}, port: {}, database: {}", host, port, database);

        // Initialize the embedding store using the properties from application.properties
        this.embeddingStore = PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(username)
                .password(password)
                .table("ragschema."+ table)
                .createTable(true)
                .dimension(dimension)
                .build();

        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    // Method to return the embedding for a given text
    public List<Float> getEmbeddingForText(String text) {
        logger.info("Generating embedding for text: {}", text);
         Response<Embedding> embeddingResponse= embeddingModel.embed(text);
        // Return the embedding as a list of floats
        return embeddingResponse.content().vectorAsList();
    }

    // Method to ingest document into embedding store with metadata
    public void ingestDocument(String filePath, Map<String, String> metadata) throws Exception {
        logger.info("Ingesting document from filePath: {} with metadata: {}", filePath, metadata);

        Path path = Paths.get(filePath);
        Document document = FileSystemDocumentLoader.loadDocument(path, new ApachePdfBoxDocumentParser());

        // Add metadata
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            document.metadata().add(entry.getKey(), entry.getValue());
        }

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))  // Split document into segments
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // Ingest the document into the embeddings store
        ingestor.ingest(document);
        logger.info("Document ingested successfully into the embeddings store.");
    }

    // Method to search embeddings for the most relevant matches
    public List<String> search(String queryText, Map<String, String> metadataFilter) {
        logger.info("Searching embeddings for query: {} with metadataFilter: {}", queryText, metadataFilter);

        // Step 1: Embed the query text
        Embedding queryEmbedding = embeddingModel.embed(queryText).content();

        // Step 2: Find the relevant matches from the embedding store
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, 5);

        // Step 3: Filter results based on metadata
        List<String> filteredResults = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();

            // Check if the metadata matches the filter
            if (metadataMatches(segment.metadata(), metadataFilter)) {
                filteredResults.add(segment.text());
            }
        }

        logger.info("Search completed. Found {} matching results.", filteredResults.size());
        return filteredResults;
    }

    // Health check for Yugabyte connection
    public String healthCheck() {
        try (Connection connection = com.fusion.YugabyteConnectionPool.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                logger.info("YugabyteDB connection is healthy.");
                return "YugabyteDB is healthy";
            }
        } catch (SQLException e) {
            logger.error("YugabyteDB health check failed: {}", e.getMessage());
            return "YugabyteDB connection failed: " + e.getMessage();
        }
        return "YugabyteDB connection status unknown";
    }

    // Method to delete entries by metadata filter
    public void deleteByMetadata(Map<String, String> metadataFilter) throws SQLException {
        logger.info("Deleting entries from embeddings store with metadata filter: {}", metadataFilter);

        StringBuilder query = new StringBuilder("DELETE FROM document_embeddings WHERE ");
        boolean firstCondition = true;
        for (String key : metadataFilter.keySet()) {
            if (!firstCondition) {
                query.append(" AND ");
            }
            query.append("metadata->>? = ?");
            firstCondition = false;
        }

        try (Connection connection = com.fusion.YugabyteConnectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(query.toString())) {

            int index = 1;
            for (Map.Entry<String, String> entry : metadataFilter.entrySet()) {
                statement.setString(index++, entry.getKey());
                statement.setString(index++, entry.getValue());
            }
            int rowsAffected = statement.executeUpdate();
            logger.info("{} entries deleted based on metadata filter.", rowsAffected);
        }
    }

    // Method to clear all entries from the document_embeddings table
    public void clearAllEntries() throws SQLException {
        logger.info("Clearing all entries from document_embeddings table.");
        String sql = "TRUNCATE TABLE document_embeddings";
        try (Connection connection = com.fusion.YugabyteConnectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            logger.info("All entries cleared from document_embeddings table.");
        }
    }

    public boolean metadataMatches(Metadata metadata, Map<String, String> metadataFilter) {
        for (Map.Entry<String, String> entry : metadataFilter.entrySet()) {
            String key = entry.getKey();
            String expectedValue = entry.getValue();
            String actualValue = metadata.get(key);

            // If any value doesn't match, return false
            if (actualValue == null || !actualValue.equals(expectedValue)) {
                logger.debug("Metadata mismatch for key: {}. Expected: {}, Found: {}", key, expectedValue, actualValue);
                return false;
            }
        }
        return true;
    }
}