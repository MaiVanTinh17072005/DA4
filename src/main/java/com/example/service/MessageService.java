package com.example.service;

import com.example.api.dto.MessageDTO;
import com.example.config.ApiConfig;
import com.example.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing message operations
 * Handles message retrieval and sending
 */
public class MessageService {

    private final Gson gson = new Gson();

    /**
     * Get all messages for current user
     * @return List of messages
     */
    public List<MessageDTO> getAllMessages() {
        try {
            String token = SessionManager.getAuthToken();
            if (token == null || token.isEmpty()) {
                System.err.println("❌ [MessageService] No auth token found");
                return new ArrayList<>();
            }

            String urlString = ApiConfig.BASE_URL + "/api/v1/messages";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            try {
                // Configure connection
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);

                System.out.println("[MessageService] Getting all messages...");

                // Get response
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parse JSON array
                    Type listType = new TypeToken<List<MessageDTO>>(){}.getType();
                    List<MessageDTO> messages = gson.fromJson(response.toString(), listType);

                    System.out.println("✅ [MessageService] Got " + messages.size() + " messages");
                    return messages;

                } else {
                    System.err.println("❌ [MessageService] Failed to get messages. Code: " + responseCode);
                    return new ArrayList<>();
                }

            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            System.err.println("❌ [MessageService] Error getting messages: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get conversation with a specific user
     * @param userId Other user's ID
     * @return List of messages in conversation
     */
    public List<MessageDTO> getConversation(Long userId) {
        try {
            String token = SessionManager.getAuthToken();
            if (token == null || token.isEmpty()) {
                System.err.println("❌ [MessageService] No auth token found");
                return new ArrayList<>();
            }

            String urlString = ApiConfig.BASE_URL + "/api/v1/messages/conversation/" + userId;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            try {
                // Configure connection
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);

                System.out.println("[MessageService] Getting conversation with user: " + userId);

                // Get response
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parse JSON array
                    Type listType = new TypeToken<List<MessageDTO>>(){}.getType();
                    List<MessageDTO> messages = gson.fromJson(response.toString(), listType);

                    System.out.println("✅ [MessageService] Got " + messages.size() + " messages in conversation");
                    return messages;

                } else {
                    System.err.println("❌ [MessageService] Failed to get conversation. Code: " + responseCode);
                    return new ArrayList<>();
                }

            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            System.err.println("❌ [MessageService] Error getting conversation: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get messages in a group
     * @param groupId Group ID
     * @return List of messages in group
     */
    public List<MessageDTO> getGroupMessages(Long groupId) {
        try {
            String token = SessionManager.getAuthToken();
            if (token == null || token.isEmpty()) {
                System.err.println("❌ [MessageService] No auth token found");
                return new ArrayList<>();
            }

            String urlString = ApiConfig.BASE_URL + "/api/v1/messages/group/" + groupId;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            try {
                // Configure connection
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);

                System.out.println("[MessageService] Getting group messages: " + groupId);

                // Get response
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parse JSON array
                    Type listType = new TypeToken<List<MessageDTO>>(){}.getType();
                    List<MessageDTO> messages = gson.fromJson(response.toString(), listType);

                    System.out.println("✅ [MessageService] Got " + messages.size() + " group messages");
                    return messages;

                } else {
                    System.err.println("❌ [MessageService] Failed to get group messages. Code: " + responseCode);
                    return new ArrayList<>();
                }

            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            System.err.println("❌ [MessageService] Error getting group messages: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Send a new message
     * @param messageDTO Message data
     * @return Sent message DTO if successful, null otherwise
     */
    public MessageDTO sendMessage(MessageDTO messageDTO) {
        try {
            String token = SessionManager.getAuthToken();
            if (token == null || token.isEmpty()) {
                System.err.println("❌ [MessageService] No auth token found");
                return null;
            }

            String urlString = ApiConfig.BASE_URL + "/api/v1/messages/send";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            try {
                // Configure connection
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);

                // Create request body
                String jsonRequest = gson.toJson(messageDTO);

                System.out.println("[MessageService] Sending message...");

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Get response
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parse response
                    MessageDTO sentMessage = gson.fromJson(response.toString(), MessageDTO.class);

                    System.out.println("✅ [MessageService] Message sent successfully");
                    return sentMessage;

                } else {
                    System.err.println("❌ [MessageService] Failed to send message. Code: " + responseCode);
                    return null;
                }

            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            System.err.println("❌ [MessageService] Error sending message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Mark message as read
     * @param msgId Message ID
     * @return true if successful, false otherwise
     */
    public boolean markAsRead(Long msgId) {
        try {
            String token = SessionManager.getAuthToken();
            if (token == null || token.isEmpty()) {
                System.err.println("❌ [MessageService] No auth token found");
                return false;
            }

            String urlString = ApiConfig.BASE_URL + "/api/v1/messages/" + msgId + "/read";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            try {
                // Configure connection
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);

                System.out.println("[MessageService] Marking message as read: " + msgId);

                // Get response
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("✅ [MessageService] Message marked as read");
                    return true;
                } else {
                    System.err.println("❌ [MessageService] Failed to mark message as read. Code: " + responseCode);
                    return false;
                }

            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            System.err.println("❌ [MessageService] Error marking message as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
