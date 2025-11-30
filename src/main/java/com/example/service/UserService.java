package com.example.service;

import com.example.api.ApiClient;
import com.example.api.dto.ChangePasswordRequest;
import com.example.api.dto.ChangePasswordResponse;
import com.example.api.dto.UpdateProfileRequest;
import com.example.api.dto.UpdateProfileResponse;
import com.example.config.ApiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Service for user-related operations
 * Handles profile updates and user management
 */
public class UserService {
    
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;
    
    public UserService() {
        this.apiClient = new ApiClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Update user profile (email, username, avatar URL)
     * @param request UpdateProfileRequest containing new profile data
     * @return UpdateProfileResponse with success status and updated user data
     * @throws IOException if network error occurs
     */
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request) throws IOException {
        System.out.println("[UserService] Updating profile: " + request);
        
        // Get JWT token from SessionManager
        String token = com.example.util.SessionManager.getAuthToken();
        if (token == null || token.isEmpty()) {
            System.out.println("[UserService] ❌ No authentication token found");
            return new UpdateProfileResponse(false, "Bạn cần đăng nhập để cập nhật thông tin", null);
        }
        
        // Set auth token in ApiClient
        apiClient.setAuthToken(token);
        System.out.println("[UserService] ✓ Auth token set in ApiClient");
        
        // Convert request to JSON
        String requestJson = objectMapper.writeValueAsString(request);
        System.out.println("[UserService] Request JSON: " + requestJson);
        
        // Make PUT request to server
        HttpURLConnection connection = null;
        try {
            connection = apiClient.put(ApiConfig.UPDATE_PROFILE_ENDPOINT, requestJson);
            
            int responseCode = connection.getResponseCode();
            System.out.println("[UserService] Response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Success - parse response
                String responseBody = apiClient.readResponse(connection);
                System.out.println("[UserService] Response body: " + responseBody);
                
                UpdateProfileResponse response = objectMapper.readValue(responseBody, UpdateProfileResponse.class);
                System.out.println("[UserService] ✓ Profile updated successfully");
                return response;
                
            } else {
                // Error - read error message
                String errorBody = apiClient.readErrorResponse(connection);
                System.out.println("[UserService] ❌ Error response: " + errorBody);
                
                // Try to parse error response
                try {
                    UpdateProfileResponse errorResponse = objectMapper.readValue(errorBody, UpdateProfileResponse.class);
                    return errorResponse;
                } catch (Exception e) {
                    // If parsing fails, create generic error response
                    return new UpdateProfileResponse(false, "Lỗi cập nhật profile: " + errorBody, null);
                }
            }
            
        } catch (Exception e) {
            System.out.println("[UserService] ❌ Network error: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Không thể kết nối đến server: " + e.getMessage(), e);
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Change user password
     * @param request ChangePasswordRequest with current and new passwords (SHA-256 hashed)
     * @return ChangePasswordResponse with success status and message
     * @throws IOException if network error occurs
     */
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) throws IOException {
        System.out.println("[UserService] Changing password...");
        
        // Get JWT token from SessionManager
        String token = com.example.util.SessionManager.getAuthToken();
        if (token == null || token.isEmpty()) {
            System.out.println("[UserService] ❌ No authentication token found");
            return new ChangePasswordResponse(false, "Bạn cần đăng nhập để đổi mật khẩu");
        }
        
        // Set auth token in ApiClient
        apiClient.setAuthToken(token);
        System.out.println("[UserService] ✓ Auth token set in ApiClient");
        
        // Convert request to JSON
        String requestJson = objectMapper.writeValueAsString(request);
        System.out.println("[UserService] Request JSON: " + request); // Don't log actual passwords
        
        // Make PUT request to server
        HttpURLConnection connection = null;
        try {
            connection = apiClient.put(ApiConfig.CHANGE_PASSWORD_ENDPOINT, requestJson);
            
            int responseCode = connection.getResponseCode();
            System.out.println("[UserService] Response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Success - parse response
                String responseBody = apiClient.readResponse(connection);
                System.out.println("[UserService] Response body: " + responseBody);
                
                ChangePasswordResponse response = objectMapper.readValue(responseBody, ChangePasswordResponse.class);
                System.out.println("[UserService] ✓ Password changed successfully");
                return response;
                
            } else {
                // Error - read error message
                String errorBody = apiClient.readErrorResponse(connection);
                System.out.println("[UserService] ❌ Error response: " + errorBody);
                
                // Try to parse error response
                try {
                    ChangePasswordResponse errorResponse = objectMapper.readValue(errorBody, ChangePasswordResponse.class);
                    return errorResponse;
                } catch (Exception e) {
                    // If parsing fails, create generic error response
                    return new ChangePasswordResponse(false, "Lỗi đổi mật khẩu: " + errorBody);
                }
            }
            
        } catch (Exception e) {
            System.out.println("[UserService] ❌ Network error: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Không thể kết nối đến server: " + e.getMessage(), e);
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
