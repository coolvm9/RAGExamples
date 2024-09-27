package com.fusion;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestClient {

    public static String getAnswer(String question, List<String> paragraphs) throws IOException {
        URL url = new URL("http://localhost:5007/get-answer");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        // Using Gson to construct the JSON payload
        Gson gson = new Gson();
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("question", question);
        payloadMap.put("paragraphs", paragraphs);
        String jsonInputString = gson.toJson(payloadMap);
        // Send the JSON data
        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            outputStream.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
        }

        // Read the response
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
        } catch (IOException e) {
            // Read error stream if present
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
            }
            throw e; // Rethrow exception after reading error stream
        } finally {
            connection.disconnect();
        }

        return response.toString();
    }

    public static void main(String[] args) {
        try {
            String question = "What is the warranty period for the powertrain?";
            // Initialize paragraphs as an ArrayList<String>
            List<String> paragraphs = new ArrayList<String>();
            paragraphs.add(" (1) Your vehicle’s Powertrain components are covered for five years or " +
                    "60,000 miles, whichever occurs first. The extended coverage applies to the Engine: " +
                    "all internal lubricated parts, cylinder block, cylinder heads, electrical fuel pump, powertrain");
            paragraphs.add("The warranty coverage period for:\n" +
                    "• Passenger cars and light duty trucks (applies to vehicles up\n" +
                    "through 8,500 pounds GVWR) is as follows:\n" +
                    "—   8 years or 80,000 miles (whichever occurs first) for catalytic\n" +
                    "converters, electronic emissions control unit, and");

            String answer = getAnswer(question, paragraphs);
            System.out.println("Answer received: " + answer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
