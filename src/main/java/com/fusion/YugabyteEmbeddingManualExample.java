package com.fusion;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class YugabyteEmbeddingManualExample {

    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void main(String[] args) {

        try {
            String yugabyteHost = "localhost"; // Default for local setup
            int yugabytePort = 5433;  // Default YSQL port for YugabyteDB
            String dbName = "postgres";  // Default database name
            String username = "yugabyte";  // Default username
            String password = "yugabyte";  // Default password is empty

            // Print connection details
            System.out.println("Connect to YugabyteDB using the following details:");
            System.out.println("Host: " + yugabyteHost);
            System.out.println("Port: " + yugabytePort);
            System.out.println("Database: " + dbName);
            System.out.println("Username: " + username);
            System.out.println("Password: " + password);

            // Connect to Yugabyte and drop/clean the table
            String jdbcUrl = "jdbc:postgresql://" + yugabyteHost + ":" + yugabytePort + "/" + dbName;
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {

                // Option 1: TRUNCATE the table if it exists
                statement.executeUpdate("TRUNCATE TABLE public.document_embeddings;");

                // Option 2: DROP the table if you want to fully recreate it (uncomment this if needed)
                // statement.executeUpdate("DROP TABLE IF EXISTS public.document_embeddings;");

                System.out.println("Existing data cleared from document_embeddings table.");
            }

            // Set up the PgVectorEmbeddingStore (since Yugabyte uses the PostgreSQL protocol)
            EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                    .host(yugabyteHost)
                    .port(yugabytePort)
                    .database(dbName)
                    .user(username)
                    .password(password)
                    .table("document_embeddings")  // Ensure the table exists or create it
                    .dimension(384)  // Must match the dimension of the embedding model
                    .build();

            // Initialize the embedding model
            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            // Ingestor setup
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(300, 0))  // Split documents into segments
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            // Load the PDF file
            Path filePath = toPath("example-files/2025_US_F150_Warranty_Guide_ENG_V1.pdf");

            Document document = FileSystemDocumentLoader.loadDocument(filePath, new ApachePdfBoxDocumentParser());

            // Add metadata
            document.metadata().add("fileName", filePath.getFileName().toString());
            document.metadata().add("filePath", filePath.toString());
            document.metadata().add("company", "FORD");
            document.metadata().add("product", "F150");
            document.metadata().add("language", "ENG");
            document.metadata().add("version", "V1");
            document.metadata().add("year", "2025");
            document.metadata().add("type", "Warranty Guide");
            document.metadata().add("country", "US");
            document.metadata().add("category", "Automotive");

            // Ingest document into PostgreSQL pgvector
            ingestor.ingest(document);


            filePath = toPath("example-files/Tesla_Models_Owners_Manual.pdf");

            document = FileSystemDocumentLoader.loadDocument(filePath, new ApachePdfBoxDocumentParser());

            // Add metadata
            document.metadata().add("fileName", filePath.getFileName().toString());
            document.metadata().add("filePath", filePath.toString());
            document.metadata().add("company", "TESLA");
            document.metadata().add("product", "Model ");
            document.metadata().add("language", "ENG");
            document.metadata().add("version", "V1");
            document.metadata().add("year", "2021");
            document.metadata().add("type", "Warranty Guide");
            document.metadata().add("country", "US");
            document.metadata().add("category", "Automotive");

            // Ingest document into PostgreSQL pgvector
            ingestor.ingest(document);

            // Query loop
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter your query (or type 'exit' to quit):");

                String query = scanner.nextLine();

                if ("exit".equalsIgnoreCase(query)) {
                    System.out.println("Exiting program.");
                    break;
                }

                // Embed the query
                Embedding queryEmbedding = embeddingModel.embed(query).content();

                // Find relevant matches from PostgreSQL
                List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 5);
                System.out.println("Start ---------   Matching Context from Document: Ford_Warranty_Guide.pdf");

                List<String> answers = new ArrayList<>();
                for (EmbeddingMatch<TextSegment> match : relevant) {
                    System.out.println(match.score());
                    answers.add(match.embedded().text());
                    System.out.println(ANSI_GREEN + match.embedded().text() + ANSI_RESET);
                    System.out.println("");
                }
                System.out.println("End ---------   Matching Context from Document: Ford_Warranty_Guide.pdf");
            }

            // Close the scanner
            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path toPath(String fileName) {
        try {
            // Corrected path assuming files are in src/main/resources/example-files
            URL fileUrl = ElasticSearchEmbeddingManualExample.class.getClassLoader().getResource( fileName);
            if (fileUrl == null) {
                throw new RuntimeException("Resource not found: " + fileName);
            }
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to resolve URI for: " + fileName, e);
        }
    }

    private static boolean tableExists(Connection connection, String schemaName, String tableName) throws SQLException {
    String query = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                   "WHERE table_schema = ? AND table_name = ?)";
    try (PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setString(1, schemaName);
        statement.setString(2, tableName);
        try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        }
    }
    return false;
}
}
