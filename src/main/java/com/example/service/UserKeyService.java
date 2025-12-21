package com.example.service;

import com.example.api.ApiClient;
import com.example.api.dto.PublicKeyDTO;
import com.example.config.ApiConfig;
import com.example.util.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service for managing user public keys for E2EE (Frontend)
 */
public class UserKeyService {
    
    private static final String BASE_URL = ApiConfig.BASE_URL + "/api/v1/keys";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Upload user's public key to the server
     */
    public static boolean uploadPublicKey(String publicKey) {
        try {
            Long userId = SessionManager.getCurrentUserId();
            PublicKeyDTO dto = new PublicKeyDTO(userId, publicKey);
            String json = mapper.writeValueAsString(dto);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/upload"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + SessionManager.getAuthToken())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("[UserKeyService] Error uploading public key: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Fetch friend's public key from the server
     */
    public static String fetchFriendPublicKey(Long friendId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/user/" + friendId))
                    .header("Authorization", "Bearer " + SessionManager.getAuthToken())
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                PublicKeyDTO dto = mapper.readValue(response.body(), PublicKeyDTO.class);
                return dto.getPublicKey();
            }
        } catch (Exception e) {
            System.err.println("[UserKeyService] Error fetching public key for user " + friendId + ": " + e.getMessage());
        }
        return null;
    }
}
