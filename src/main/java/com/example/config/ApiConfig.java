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
    public static final String VERIFY_OTP_ENDPOINT = AUTH_BASE + "/verify-otp";
    public static final String RESET_PASSWORD_ENDPOINT = AUTH_BASE + "/reset-password";
    public static final String UPDATE_PROFILE_ENDPOINT = AUTH_BASE + "/update-profile"; // PUT request
    public static final String CHANGE_PASSWORD_ENDPOINT = AUTH_BASE + "/change-password"; // PUT request

    // User Endpoints
    public static final String USER_BASE = API_VERSION + "/users";
    public static final String USER_PROFILE_ENDPOINT = USER_BASE + "/profile";

    // P2P Endpoints
    public static final String P2P_BASE = API_VERSION + "/p2p";
    public static final String P2P_REGISTER_ENDPOINT = P2P_BASE + "/register"; // POST request
    public static final String P2P_PEER_ENDPOINT = P2P_BASE + "/peer"; // GET request with /{userId}

    // Friend Endpoints
    public static final String FRIEND_BASE = API_VERSION + "/friends";
    public static final String FRIEND_SUGGESTIONS_ENDPOINT = FRIEND_BASE + "/suggestions"; // GET request with ?limit=10
    public static final String FRIEND_SEARCH_ENDPOINT = FRIEND_BASE + "/search"; // GET request with ?query=...
    public static final String FRIEND_REQUEST_ENDPOINT = FRIEND_BASE + "/request"; // POST request with targetId
    public static final String FRIEND_CANCEL_REQUEST_ENDPOINT = FRIEND_BASE + "/cancel"; // DELETE request with /{targetId}
    public static final String FRIEND_PENDING_ENDPOINT = FRIEND_BASE + "/pending"; // GET request - get pending requests
    public static final String FRIEND_ACCEPT_ENDPOINT = FRIEND_BASE + "/accept"; // POST request with requestId
    public static final String FRIEND_REJECT_ENDPOINT = FRIEND_BASE + "/reject"; // POST request with requestId
    public static final String GET_NOTIFICATIONS = FRIEND_BASE + "/notifications"; // GET request - get notifications
    public static final String MARK_NOTIFICATION_READ = FRIEND_BASE + "/notifications/"; // DELETE request - mark as read + notificationId

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
