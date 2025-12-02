package com.example.service;

import com.example.api.dto.UserDTO;
import com.example.config.ApiConfig;
import com.example.util.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing friend operations
 * Handles friend search, suggestions, and friend requests
 */
public class FriendService {

    private final Gson gson = new Gson();

    /**
     * Search users by username or email
     * @param query Search query (username or email)
     * @return List of matching users
     */
    public List<UserDTO> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Encode query parameter
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlString = ApiConfig.getFullUrl(ApiConfig.FRIEND_SEARCH_ENDPOINT) + "?query=" + encodedQuery;
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            try {
                // Configure connection
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                    ApiConfig.BEARER_PREFIX + SessionManager.getAuthToken());
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
                
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
                    Type listType = new TypeToken<List<UserDTO>>(){}.getType();
                    List<UserDTO> users = gson.fromJson(response.toString(), listType);
                    
                    System.out.println("✅ [FriendService] Found " + users.size() + " users matching: " + query);
                    return users;
                    
                } else {
                    System.err.println("❌ [FriendService] Search failed with code: " + responseCode);
                    return new ArrayList<>();
                }
                
            } finally {
                conn.disconnect();
            }
            
        } catch (Exception e) {
            System.err.println("❌ [FriendService] Error searching users: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get random non-friend users for suggestions
     * @param limit Maximum number of suggestions
     * @return List of suggested users
     */
    public List<UserDTO> getRandomNonFriends(int limit) {
        try {
            String urlString = ApiConfig.getFullUrl(ApiConfig.FRIEND_SUGGESTIONS_ENDPOINT) + "?limit=" + limit;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            try {
                // Configure connection
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                    ApiConfig.BEARER_PREFIX + SessionManager.getAuthToken());
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
                
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
                    Type listType = new TypeToken<List<UserDTO>>(){}.getType();
                    List<UserDTO> users = gson.fromJson(response.toString(), listType);
                    
                    System.out.println("✅ [FriendService] Got " + users.size() + " friend suggestions");
                    return users;
                    
                } else {
                    System.err.println("❌ [FriendService] Suggestions failed with code: " + responseCode);
                    return new ArrayList<>();
                }
                
            } finally {
                conn.disconnect();
            }
            
        } catch (Exception e) {
            System.err.println("❌ [FriendService] Error getting suggestions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Get friends list
     */
    public List<UserDTO> getFriendsList() {
        try {
            String token = SessionManager.getAuthToken();
            
            String url = ApiConfig.BASE_URL + ApiConfig.GET_FRIENDS_LIST;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
            conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
            
            int responseCode = conn.getResponseCode();
            System.out.println("✅ [FriendService] Get friends list response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                // Parse JSON array
                Type listType = new TypeToken<List<UserDTO>>(){}.getType();
                List<UserDTO> friends = new Gson().fromJson(response.toString(), listType);
                
                System.out.println("✅ [FriendService] Got " + friends.size() + " friends");
                return friends;
            } else {
                System.err.println("❌ [FriendService] Failed to get friends list: " + responseCode);
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            System.err.println("❌ [FriendService] Error getting friends list: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Send friend request to a user
     * @param userId ID of the user to send request to
     * @return true if successful, false otherwise
     */
    public boolean sendFriendRequest(Long userId) {
        try {
            URL url = new URL(ApiConfig.getFullUrl(ApiConfig.FRIEND_REQUEST_ENDPOINT));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            try {
                // Configure connection
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                    ApiConfig.BEARER_PREFIX + SessionManager.getAuthToken());
                conn.setDoOutput(true);
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
                
                // Create request body
                Map<String, Long> requestBody = new HashMap<>();
                requestBody.put("targetId", userId);
                String jsonRequest = gson.toJson(requestBody);
                
                System.out.println("[FriendService] Sending friend request to user: " + userId);
                
                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                // Get response
                int responseCode = conn.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response body
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    System.out.println("✅ [FriendService] Server response: " + response.toString());
                    System.out.println("✅ [FriendService] Friend request sent successfully");
                    return true;
                } else {
                    // Read error response
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    
                    System.err.println("❌ [FriendService] Failed to send friend request");
                    System.err.println("   Response code: " + responseCode);
                    System.err.println("   Error: " + errorResponse.toString());
                    return false;
                }
                
            } finally {
                conn.disconnect();
            }
            
        } catch (Exception e) {
            System.err.println("❌ [FriendService] Error sending friend request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Cancel a friend request
     * @param userId ID of the user to cancel request for
     * @return true if successful, false otherwise
     */
    public boolean cancelFriendRequest(Long userId) {
        try {
            String urlString = ApiConfig.getFullUrl(ApiConfig.FRIEND_CANCEL_REQUEST_ENDPOINT) + "/" + userId;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            try {
                // Configure connection
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                    ApiConfig.BEARER_PREFIX + SessionManager.getAuthToken());
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
                
                System.out.println("[FriendService] Cancelling friend request for user: " + userId);
                
                // Get response
                int responseCode = conn.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response body
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    System.out.println("✅ [FriendService] Server response: " + response.toString());
                    System.out.println("✅ [FriendService] Friend request cancelled successfully");
                    return true;
                } else {
                    // Read error response
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    
                    System.err.println("❌ [FriendService] Failed to cancel friend request");
                    System.err.println("   Response code: " + responseCode);
                    System.err.println("   Error: " + errorResponse.toString());
                    return false;
                }
                
            } finally {
                conn.disconnect();
            }
            
        } catch (Exception e) {
            System.err.println("❌ [FriendService] Error cancelling friend request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get pending friend requests
     * @return List of pending friend requests
     */
    public List<com.example.api.dto.PendingFriendRequestDTO> getPendingRequests() {
        try {
            String urlString = ApiConfig.getFullUrl(ApiConfig.FRIEND_PENDING_ENDPOINT);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            try {
                // Configure connection
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                    ApiConfig.BEARER_PREFIX + SessionManager.getAuthToken());
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
                
                System.out.println("[FriendService] Getting pending friend requests");
                
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
                    Type listType = new TypeToken<List<com.example.api.dto.PendingFriendRequestDTO>>(){}.getType();
                    List<com.example.api.dto.PendingFriendRequestDTO> requests = gson.fromJson(response.toString(), listType);
                    
                    System.out.println("✅ [FriendService] Got " + requests.size() + " pending requests");
                    return requests;
                    
                } else {
                    System.err.println("❌ [FriendService] Failed to get pending requests. Code: " + responseCode);
                    return new ArrayList<>();
                }
                
            } finally {
                conn.disconnect();
            }
            
        } catch (Exception e) {
            System.err.println("❌ [FriendService] Error getting pending requests: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Accept friend request
     * @param requestId Request ID to accept
     * @return true if successful, false otherwise
     */
    public boolean acceptFriendRequest(String requestId) {
        try {
            URL url = new URL(ApiConfig.getFullUrl(ApiConfig.FRIEND_ACCEPT_ENDPOINT));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            try {
                // Configure connection
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                    ApiConfig.BEARER_PREFIX + SessionManager.getAuthToken());
                conn.setDoOutput(true);
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
                
                // Create request body
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("requestId", requestId);
                String jsonRequest = gson.toJson(requestBody);
                
                System.out.println("[FriendService] Accepting friend request: " + requestId);
                
                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                // Get response
                int responseCode = conn.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("✅ [FriendService] Friend request accepted successfully");
                    return true;
                } else {
                    System.err.println("❌ [FriendService] Failed to accept friend request. Code: " + responseCode);
                    return false;
                }
                
            } finally {
                conn.disconnect();
            }
            
        } catch (Exception e) {
            System.err.println("❌ [FriendService] Error accepting friend request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Reject friend request
     * @param requestId Request ID to reject
     * @return true if successful, false otherwise
     */
    public boolean rejectFriendRequest(String requestId) {
        try {
            URL url = new URL(ApiConfig.getFullUrl(ApiConfig.FRIEND_REJECT_ENDPOINT));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            try {
                // Configure connection
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                    ApiConfig.BEARER_PREFIX + SessionManager.getAuthToken());
                conn.setDoOutput(true);
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
                
                // Create request body
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("requestId", requestId);
                String jsonRequest = gson.toJson(requestBody);
                
                System.out.println("[FriendService] Rejecting friend request: " + requestId);
                
                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                // Get response
                int responseCode = conn.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("✅ [FriendService] Friend request rejected successfully");
                    return true;
                } else {
                    System.err.println("❌ [FriendService] Failed to reject friend request. Code: " + responseCode);
                    return false;
                }
                
            } finally {
                conn.disconnect();
            }
            
        } catch (Exception e) {
            System.err.println("❌ [FriendService] Error rejecting friend request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get friend request notifications
     */
    public List<com.example.api.dto.FriendNotificationDTO> getNotifications() {
        try {
            String token = SessionManager.getAuthToken();
            
            String url = ApiConfig.BASE_URL + ApiConfig.GET_NOTIFICATIONS;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                // Parse JSON array
                ObjectMapper mapper = new ObjectMapper();
                List<com.example.api.dto.FriendNotificationDTO> notifications = mapper.readValue(
                    response.toString(),
                    mapper.getTypeFactory().constructCollectionType(
                        List.class, com.example.api.dto.FriendNotificationDTO.class)
                );
                
                System.out.println("[FriendService] ✅ Retrieved " + notifications.size() + " notifications");
                return notifications;
            } else {
                System.err.println("[FriendService] ❌ Failed to get notifications: " + responseCode);
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error getting notifications: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Mark notification as read
     */
    public boolean markNotificationAsRead(String notificationId) {
        try {
            String token = SessionManager.getAuthToken();
            
            String url = ApiConfig.BASE_URL + ApiConfig.MARK_NOTIFICATION_READ + notificationId;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("[FriendService] ✅ Notification marked as read");
                return true;
            } else {
                System.err.println("[FriendService] ❌ Failed to mark notification as read: " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error marking notification as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Remove friend
     * @param friendId ID of the friend to remove
     * @return true if successful, false otherwise
     */
    public boolean removeFriend(Long friendId) {
        try {
            String urlString = ApiConfig.getFullUrl(ApiConfig.FRIEND_REMOVE_ENDPOINT) + "/" + friendId;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            try {
                // Configure connection
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
                conn.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                    ApiConfig.BEARER_PREFIX + SessionManager.getAuthToken());
                conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
                conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
                
                System.out.println("[FriendService] Removing friend: " + friendId);
                
                // Get response
                int responseCode = conn.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response body
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    System.out.println("✅ [FriendService] Server response: " + response.toString());
                    System.out.println("✅ [FriendService] Friend removed successfully");
                    return true;
                } else {
                    // Read error response
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    
                    System.err.println("❌ [FriendService] Failed to remove friend");
                    System.err.println("   Response code: " + responseCode);
                    System.err.println("   Error: " + errorResponse.toString());
                    return false;
                }
                
            } finally {
                conn.disconnect();
            }
            
        } catch (Exception e) {
            System.err.println("❌ [FriendService] Error removing friend: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}


