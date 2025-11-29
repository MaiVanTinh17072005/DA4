package com.example;

import com.example.api.dto.AuthResponse;
import com.example.service.AuthService;
import com.example.util.SessionManager;

/**
 * Example usage of REST API Client
 * This demonstrates how to use AuthService for registration and login
 */
public class ApiUsageExample {
    
    public static void main(String[] args) {
        // Example 1: Register a new user
        registerExample();
        
        // Example 2: Login with existing user
        loginExample();
        
        // Example 3: Check session
        sessionExample();
    }
    
    /**
     * Example: Register a new user
     */
    private static void registerExample() {
        System.out.println("=== REGISTRATION EXAMPLE ===");
        
        try {
            // Call AuthService to register
            // Password will be automatically hashed with SHA-256
            AuthResponse response = AuthService.getInstance().register(
                "newuser@example.com",  // email
                "newusername",          // username
                "mypassword123"         // password (will be hashed)
            );
            
            // Check if successful
            if (response.isSuccess()) {
                System.out.println("✓ Registration successful!");
                System.out.println("  Message: " + response.getMessage());
                System.out.println("  User ID: " + response.getUser().getId());
                System.out.println("  Username: " + response.getUser().getUsername());
                System.out.println("  Token: " + response.getToken());
            } else {
                System.out.println("✗ Registration failed!");
                System.out.println("  Error: " + response.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error during registration:");
            System.err.println("  " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * Example: Login with existing user
     */
    private static void loginExample() {
        System.out.println("=== LOGIN EXAMPLE ===");
        
        try {
            // Call AuthService to login
            // Password will be automatically hashed with SHA-256
            AuthResponse response = AuthService.getInstance().login(
                "user@example.com",     // email
                "password123"           // password (will be hashed)
            );
            
            // Check if successful
            if (response.isSuccess()) {
                System.out.println("✓ Login successful!");
                System.out.println("  Message: " + response.getMessage());
                System.out.println("  User ID: " + response.getUser().getId());
                System.out.println("  Username: " + response.getUser().getUsername());
                System.out.println("  Email: " + response.getUser().getEmail());
                System.out.println("  Token: " + response.getToken());
                
                // Save session
                SessionManager.setCurrentUser(response.getUser());
                SessionManager.setAuthToken(response.getToken());
                System.out.println("  Session saved!");
                
            } else {
                System.out.println("✗ Login failed!");
                System.out.println("  Error: " + response.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error during login:");
            System.err.println("  " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * Example: Check and use session
     */
    private static void sessionExample() {
        System.out.println("=== SESSION EXAMPLE ===");
        
        // Check if user is logged in
        if (SessionManager.isLoggedIn()) {
            System.out.println("✓ User is logged in!");
            System.out.println("  User ID: " + SessionManager.getCurrentUserId());
            System.out.println("  Username: " + SessionManager.getCurrentUsername());
            System.out.println("  Email: " + SessionManager.getCurrentUserEmail());
            System.out.println("  Token: " + SessionManager.getAuthToken());
            
            // Logout
            System.out.println("\nLogging out...");
            SessionManager.clearSession();
            AuthService.getInstance().logout();
            System.out.println("✓ Logged out successfully!");
            
        } else {
            System.out.println("✗ No user is logged in");
        }
        
        System.out.println();
    }
    
    /**
     * Example: Password hashing demonstration
     */
    private static void passwordHashExample() {
        System.out.println("=== PASSWORD HASH EXAMPLE ===");
        
        String plainPassword = "password123";
        System.out.println("Plain password: " + plainPassword);
        
        // The password will be hashed like this internally:
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            System.out.println("Hashed password (SHA-256): " + hexString.toString());
            System.out.println("\nThis hashed password is what gets sent to the server!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println();
    }
}
