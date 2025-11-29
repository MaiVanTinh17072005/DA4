package com.example.service;

import com.example.api.ApiClient;
import com.example.api.dto.AuthResponse;
import com.example.api.dto.LoginRequest;
import com.example.api.dto.RegisterRequest;
import com.example.config.ApiConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Authentication Service
 * Handles user login and registration with the backend server
 */
public class AuthService {
    
    private final ApiClient apiClient;
    private static AuthService instance;
    
    /**
     * Private constructor for singleton pattern
     */
    private AuthService() {
        this.apiClient = new ApiClient();
    }
    
    /**
     * Get singleton instance
     */
    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    
    /**
     * Register new user
     * 
     * @param email User email
     * @param username User username
     * @param password User password (will be hashed before sending)
     * @return AuthResponse from server
     * @throws Exception if registration fails
     */
    public AuthResponse register(String email, String username, String password) throws Exception {
        System.out.println("Registering user with email: " + email + ", username: " + username);
        // Hash password before sending
        String hashedPassword = hashPassword(password);
        
        // Create request
        RegisterRequest request = new RegisterRequest(email, username, hashedPassword);
        
        // Send to server
        AuthResponse response = apiClient.post(
                ApiConfig.REGISTER_ENDPOINT, 
                request, 
                AuthResponse.class
        );
        System.out.println("Registration response: " + response);
        
        // Store token if successful
        if (response.isSuccess() && response.getToken() != null) {
            apiClient.setAuthToken(response.getToken());
        }
        
        return response;
    }
    
    /**
     * Login user
     * 
     * @param email User email
     * @param password User password (will be hashed before sending)
     * @return AuthResponse from server
     * @throws Exception if login fails
     */
    public AuthResponse login(String email, String password) throws Exception {
        // Hash password before sending
        String hashedPassword = hashPassword(password);
        
        // Create request
        LoginRequest request = new LoginRequest(email, hashedPassword);
        
        // Send to server
        AuthResponse response = apiClient.post(
                ApiConfig.LOGIN_ENDPOINT, 
                request, 
                AuthResponse.class
        );
        
        // Store token if successful
        if (response.isSuccess() && response.getToken() != null) {
            apiClient.setAuthToken(response.getToken());
        }
        
        return response;
    }
    
    /**
     * Logout user
     */
    public void logout() {
        apiClient.clearAuthToken();
    }
    
    /**
     * Hash password using SHA-256
     * 
     * @param password Plain text password
     * @return Hashed password in hexadecimal format
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Get API client (for advanced usage)
     */
    public ApiClient getApiClient() {
        return apiClient;
    }
}
