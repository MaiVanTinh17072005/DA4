package com.example.service;

import com.example.api.ApiClient;
import com.example.api.dto.AuthResponse;
import com.example.api.dto.ForgotPasswordRequest;
import com.example.api.dto.ForgotPasswordResponse;
import com.example.api.dto.LoginRequest;
import com.example.api.dto.RegisterRequest;
import com.example.api.dto.ResetPasswordRequest;
import com.example.api.dto.ResetPasswordResponse;
import com.example.api.dto.VerifyOtpRequest;
import com.example.api.dto.VerifyOtpResponse;
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
     * Request password reset - sends OTP to user's email
     * 
     * @param email User email
     * @return ForgotPasswordResponse from server
     * @throws Exception if request fails
     */
    public ForgotPasswordResponse forgotPassword(String email) throws Exception {
        System.out.println("Requesting password reset for email: " + email);
        
        // Create request
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        
        // Send to server
        ForgotPasswordResponse response = apiClient.post(
                ApiConfig.FORGOT_PASSWORD_ENDPOINT,
                request,
                ForgotPasswordResponse.class
        );
        
        System.out.println("Forgot password response: " + response);
        
        return response;
    }
    
    /**
     * Verify OTP code
     * 
     * @param email User email
     * @param otp OTP code to verify
     * @return VerifyOtpResponse from server
     * @throws Exception if request fails
     */
    public VerifyOtpResponse verifyOtp(String email, String otp) throws Exception {
        System.out.println("Verifying OTP for email: " + email);
        
        // Create request
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        
        // Send to server
        VerifyOtpResponse response = apiClient.post(
                ApiConfig.VERIFY_OTP_ENDPOINT,
                request,
                VerifyOtpResponse.class
        );
        
        System.out.println("Verify OTP response: " + response);
        
        return response;
    }
    
    /**
     * Reset password with new password
     * 
     * @param email User email
     * @param newPassword New password (will be hashed before sending)
     * @return ResetPasswordResponse from server
     * @throws Exception if request fails
     */
    public ResetPasswordResponse resetPassword(String email, String newPassword) throws Exception {
        System.out.println("Resetting password for email: " + email);
        
        // Hash password before sending
        String hashedPassword = hashPassword(newPassword);
        
        // Create request
        ResetPasswordRequest request = new ResetPasswordRequest(email, hashedPassword);
        
        // Send to server
        ResetPasswordResponse response = apiClient.post(
                ApiConfig.RESET_PASSWORD_ENDPOINT,
                request,
                ResetPasswordResponse.class
        );
        
        System.out.println("Reset password response: " + response);
        
        return response;
    }
    
    /**
     * Logout user
     * Sends logout request to server to update status to offline
     * 
     * @throws Exception if logout request fails
     */
    public void logout() throws Exception {
        try {
            System.out.println("Logging out user...");
            
            // Send logout request to server to update status to offline
            // The server will use the auth token to identify the user
            apiClient.post(
                    ApiConfig.LOGOUT_ENDPOINT,
                    new Object(), // Empty body, server uses token to identify user
                    AuthResponse.class
            );
            
            System.out.println("Logout successful - status updated to offline");
        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            // Continue with local logout even if server request fails
        } finally {
            // Always clear local token
            apiClient.clearAuthToken();
        }
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
