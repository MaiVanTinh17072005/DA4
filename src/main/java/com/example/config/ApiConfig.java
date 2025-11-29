package com.example.config;

/**
 * API Configuration
 * Contains base URL and endpoint paths
 */
public class ApiConfig {
    
    // Base URL của server (có thể thay đổi theo môi trường)
    public static final String BASE_URL = "http://localhost:8080";
    
    // API Version
    public static final String API_VERSION = "/api/v1";
    
    // Auth Endpoints
    public static final String AUTH_BASE = API_VERSION + "/auth";
    public static final String LOGIN_ENDPOINT = AUTH_BASE + "/login";
    public static final String REGISTER_ENDPOINT = AUTH_BASE + "/register";
    public static final String LOGOUT_ENDPOINT = AUTH_BASE + "/logout";
    public static final String FORGOT_PASSWORD_ENDPOINT = AUTH_BASE + "/forgot-password";
    
    // User Endpoints
    public static final String USER_BASE = API_VERSION + "/users";
    public static final String USER_PROFILE_ENDPOINT = USER_BASE + "/profile";
    
    // Timeout settings (milliseconds)
    public static final int CONNECTION_TIMEOUT = 30000; // 30 seconds (increased for email sending)
    public static final int READ_TIMEOUT = 30000; // 30 seconds (increased for email sending)
    
    // Headers
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    /**
     * Get full URL for endpoint
     */
    public static String getFullUrl(String endpoint) {
        return BASE_URL + endpoint;
    }
}
