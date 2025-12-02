package com.example.util;

import com.example.api.dto.UserDTO;

/**
 * Session Manager
 * Manages current user session and authentication state
 */
public class SessionManager {
    
    private static UserDTO currentUser;
    private static String authToken;
    
    // P2P Port Information
    private static int tcpPort = -1;
    private static int udpPort = -1;
    
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
        // Stop notification polling to prevent JWT errors
        try {
            NotificationPoller.getInstance().stopPolling();
            System.out.println("[SessionManager] ✓ Stopped notification polling");
        } catch (Exception e) {
            System.err.println("[SessionManager] ⚠ Error stopping notification polling: " + e.getMessage());
        }
        
        currentUser = null;
        authToken = null;
        tcpPort = -1;
        udpPort = -1;
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
    
    /**
     * Set P2P ports for current session
     */
    public static void setP2PPorts(int tcp, int udp) {
        tcpPort = tcp;
        udpPort = udp;
    }
    
    /**
     * Get TCP port for P2P
     */
    public static int getTcpPort() {
        return tcpPort;
    }
    
    /**
     * Get UDP port for P2P
     */
    public static int getUdpPort() {
        return udpPort;
    }
}
