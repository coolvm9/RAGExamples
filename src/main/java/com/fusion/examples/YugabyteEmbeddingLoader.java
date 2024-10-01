package com.fusion.examples;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class YugabyteEmbeddingLoader {

    public static void main(String[] args) {

        try {
            String yugabyteHost = "localhost"; // Default for local setup
            int yugabytePort = 5433;  // Default YSQL port for YugabyteDB
            String dbName = "postgres";  // Default database name
            String username = "yugabyte";  // Default username
            String password = "yugabyte";  // Default password is empty

            // Connect to Yugabyte and drop/clean the table
            String jdbcUrl = "jdbc:postgresql://" + yugabyteHost + ":" + yugabytePort + "/" + dbName;
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {

                // Option 1: TRUNCATE the table if it exists
                statement.executeUpdate("TRUNCATE TABLE public.document_embeddings;");
                System.out.println("Existing data cleared from document_embeddings table.");
            }

            // Set up the PgVectorEmbeddingStore (since Yugabyte uses the PostgreSQL protocol)
            EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                    .host(yugabyteHost)
                    .port(yugabytePort)
                    .database(dbName)
                    .user(username)
                    .password(password)
                    .table("document_embeddings")
                    .createTable(true)
                    .dimension(384)
                    .build();

            // Initialize the embedding model
            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            // Ingestor setup
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(300, 0))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            // Load the PDF file and metadata
            loadDocument(ingestor, "example-files/2025_US_F150_Warranty_Guide_ENG_V1.pdf", "FORD", "F150", "2025", "Warranty Guide");
            loadDocument(ingestor, "example-files/Tesla_Models_Owners_Manual.pdf", "TESLA", "Model S", "2021", "Owner's Manual");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadDocument(EmbeddingStoreIngestor ingestor, String filePathStr, String company, String product, String year, String type) throws Exception {
        Path filePath = toPath(filePathStr);

        Document document = FileSystemDocumentLoader.loadDocument(filePath, new ApachePdfBoxDocumentParser());
        document.metadata().add("fileName", filePath.getFileName().toString());
        document.metadata().add("company", company);
        document.metadata().add("product", product);
        document.metadata().add("year", year);
        document.metadata().add("type", type);

        // Ingest document into the database
        ingestor.ingest(document);
        System.out.println("Document loaded: " + filePathStr);
    }

    private static Path toPath(String fileName) throws URISyntaxException {
        URL fileUrl = YugabyteEmbeddingLoader.class.getClassLoader().getResource(fileName);
        if (fileUrl == null) {
            throw new RuntimeException("Resource not found: " + fileName);
        }
        return Paths.get(fileUrl.toURI());
    }
}