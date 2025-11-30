package dto;

/**
 * Request DTO for updating user profile
 * Received from client with updated email, username, and avatar URL
 */
public class UpdateProfileRequest {
    
    private String email;
    private String username;
    private String avatarUrl;
    
    // Constructors
    public UpdateProfileRequest() {
    }
    
    public UpdateProfileRequest(String email, String username, String avatarUrl) {
        this.email = email;
        this.username = username;
        this.avatarUrl = avatarUrl;
    }
    
    // Getters and Setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    @Override
    public String toString() {
        return "UpdateProfileRequest{" +
                "email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                '}';
    }
}
