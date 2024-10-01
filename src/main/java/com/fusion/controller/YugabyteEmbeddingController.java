package com.fusion.controller;

import com.fusion.service.YugabyteEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/yugabyte")
public class YugabyteEmbeddingController extends EmbeddingControllerBase {

    private static final Logger logger = LoggerFactory.getLogger(YugabyteEmbeddingController.class);

    private final YugabyteEmbeddingService yugabyteEmbeddingService;

    @Autowired
    public YugabyteEmbeddingController(YugabyteEmbeddingService yugabyteEmbeddingService) {
        this.yugabyteEmbeddingService = yugabyteEmbeddingService;
    }

    @PostMapping("/embeddings/ingest")
    public ResponseEntity<String> ingestDocumentToYugabyte(
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> metadata) {
        try {
            // Save the uploaded file to a temporary location
            Path tempFile = Files.createTempFile(null, null);
            file.transferTo(tempFile.toFile());

            // Call the service to ingest the document using the temporary file path
            yugabyteEmbeddingService.ingestDocument(tempFile.toString(), metadata);

            // Delete the temporary file after ingestion
            Files.delete(tempFile);

            logger.info("Document ingested successfully.");
            return successResponse("Document ingested into Yugabyte embeddings store successfully.");
        } catch (Exception e) {
            logger.error("Error ingesting document to Yugabyte", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    // Updated Endpoint to search Yugabyte embeddings based on query and optional metadata filter
    @Override
    @PostMapping("/embeddings/search")
    public ResponseEntity<List<String>> search(
            @RequestBody Map<String, Object> searchRequest) {
        try {
            // Extracting the query and metadata filter from the request body
            String query = (String) searchRequest.get("query");
            Map<String, String> metadataFilter = (Map<String, String>) searchRequest.get("metadataFilter");

            logger.info("Searching embeddings in Yugabyte: query={}, metadataFilter={}", query, metadataFilter);

            // Performing the search with the query and metadata filter
            List<String> results = yugabyteEmbeddingService.search(query, metadataFilter);

            logger.info("Search completed successfully, results found: {}", results.size());
            return ResponseEntity.ok(results);  // Returning List<String> wrapped in ResponseEntity
        } catch (Exception e) {
            logger.error("Error searching embeddings in Yugabyte", e);
            return errorResponse(e);  // Returns error response
        }
    }

    // Endpoint to get embedding for a given text
    @PostMapping("/embeddings/getEmbedding")
    public ResponseEntity<List<Float>> getEmbedding(
            @RequestParam("text") String text) {
        try {
            logger.info("Getting embedding for text: {}", text);
            List<Float> embedding = yugabyteEmbeddingService.getEmbeddingForText(text);
            return ResponseEntity.ok(embedding);
        } catch (Exception e) {
            logger.error("Error getting embedding for text", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // Endpoint to get embedding for a given text
    @PostMapping("/embeddings/getShortEmbedding")
    public ResponseEntity<List<Float>> getShortEmbedding(
            @RequestParam("text") String text, @RequestParam("length") int length){
        try {
            logger.info("Getting embedding for text: {}", text);
            List<Float> embedding = yugabyteEmbeddingService.getEmbeddingForText(text);
            List<Float> shorterEmbedding = embedding.subList(0, Math.min(embedding.size(), length));
            return ResponseEntity.ok(shorterEmbedding);
        } catch (Exception e) {
            logger.error("Error getting embedding for text", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // Endpoint to delete entries from Yugabyte embeddings based on metadata filter
    @DeleteMapping("/embeddings/delete")
    public ResponseEntity<String> deleteYugabyteByMetadata(@RequestParam Map<String, String> metadataFilter) {
        try {
            logger.info("Deleting embeddings in Yugabyte based on metadata filter: {}", metadataFilter);
            yugabyteEmbeddingService.deleteByMetadata(metadataFilter);
            logger.info("Entries deleted successfully based on metadata filter.");
            return successResponse("Entries deleted from Yugabyte embeddings based on metadata.");
        } catch (Exception e) {
            logger.error("Error deleting entries from Yugabyte embeddings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    // Endpoint to clear all entries from the Yugabyte embedding store
    @DeleteMapping("/embeddings/clear")
    public ResponseEntity<String> clearAllYugabyteEntries() {
        try {
            logger.info("Clearing all entries from Yugabyte embeddings store.");
            yugabyteEmbeddingService.clearAllEntries();
            logger.info("All entries cleared successfully.");
            return successResponse("All entries cleared from Yugabyte embeddings store.");
        } catch (Exception e) {
            logger.error("Error clearing all entries from Yugabyte embeddings store", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

   @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
    try {
        String yugabyteHealth = yugabyteEmbeddingService.healthCheck();
        logger.info("YugabyteDB connection: {}", yugabyteHealth);
        return ResponseEntity.ok(yugabyteHealth);
    } catch (Exception e) {
        logger.error("Error during YugabyteDB health check", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error checking YugabyteDB health");
    }
}
}