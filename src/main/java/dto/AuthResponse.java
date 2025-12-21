package dto;

/**
 * DTO for authentication response
 * Matches client-side AuthResponse
 */
public class AuthResponse {
    private boolean success;
    private String message;
    private String token;
    private UserDTO user;
    private String salt;
    
    // Constructors
    public AuthResponse() {
    }
    
    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public AuthResponse(boolean success, String message, String token, UserDTO user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
    }
    
    public AuthResponse(boolean success, String message, String token, UserDTO user, String salt) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
        this.salt = salt;
    }
    
    // Static factory methods for common responses
    public static AuthResponse success(String message, String token, UserDTO user) {
        return new AuthResponse(true, message, token, user);
    }
    
    public static AuthResponse error(String message) {
        return new AuthResponse(false, message);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public UserDTO getUser() {
        return user;
    }
    
    public void setUser(UserDTO user) {
        this.user = user;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
    
    @Override
    public String toString() {
        return "AuthResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", token='" + (token != null ? "[PRESENT]" : "null") + '\'' +
                ", user=" + user +
                '}';
    }
}
