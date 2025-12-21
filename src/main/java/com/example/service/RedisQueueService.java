package com.example.service;

import com.example.api.dto.MessageDTO;
import com.example.config.ApiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service to queue messages to Redis via backend API
 */
public class RedisQueueService {
    
    private static final String QUEUE_ENDPOINT = "/api/v1/messages/queue";
    private static final int TIMEOUT_MS = 5000;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Queue a message to Redis
     * @param message Message to queue
     * @return true if successfully queued, false otherwise
     */
    public boolean queueMessage(MessageDTO message) {
        try {
            String url = ApiConfig.BASE_URL + QUEUE_ENDPOINT;
            
            System.out.println("[RedisQueueService] Queueing message to Redis: " + url);
            
            // Create HTTP connection
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            
            // Convert message to JSON
            String jsonPayload = objectMapper.writeValueAsString(message);
            
            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Check response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("[RedisQueueService] ✅ Message queued to Redis successfully");
                return true;
            } else {
                System.err.println("[RedisQueueService] ❌ Failed to queue message. Response code: " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("[RedisQueueService] ❌ Error queueing message: " + e.getMessage());
            return false;
        }
    }
}
