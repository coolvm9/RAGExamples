package com.fusion.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class EmbeddingControllerBase {

    // Shared method for handling success responses
    protected ResponseEntity<String> successResponse(String message) {
        return ResponseEntity.ok(message);
    }

    // Shared method for handling error responses for list-based operations
    protected ResponseEntity<List<String>> errorResponse(Exception e) {
        e.printStackTrace();
        // Return an empty list and the error message in a suitable HTTP status
        return new ResponseEntity<>(Collections.singletonList("An error occurred: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Abstract method to be implemented by child controllers for searching
    public abstract ResponseEntity<List<String>> search(@RequestBody Map<String, Object> searchRequest);
}