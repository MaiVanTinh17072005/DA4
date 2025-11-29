package com.example.util;

import com.example.api.dto.UserDTO;

/**
 * Session Manager
 * Manages current user session and authentication state
 */
public class SessionManager {
    
    private static UserDTO currentUser;
    private static String authToken;
    
    /**
     * Set current logged-in user
     */
    public static void setCurrentUser(UserDTO user) {
        currentUser = user;
    }
    
    /**
     * Get current logged-in user
     */
    public static UserDTO getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Set authentication token
     */
    public static void setAuthToken(String token) {
        authToken = token;
    }
    
    /**
     * Get authentication token
     */
    public static String getAuthToken() {
        return authToken;
    }
    
    /**
     * Check if user is logged in
     */
    public static boolean isLoggedIn() {
        return currentUser != null && authToken != null;
    }
    
    /**
     * Clear session (logout)
     */
    public static void clearSession() {
        currentUser = null;
        authToken = null;
    }
    
    /**
     * Get current user ID
     */
    public static Long getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }
    
    /**
     * Get current username
     */
    public static String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }
    
    /**
     * Get current user email
     */
    public static String getCurrentUserEmail() {
        return currentUser != null ? currentUser.getEmail() : null;
    }
}
