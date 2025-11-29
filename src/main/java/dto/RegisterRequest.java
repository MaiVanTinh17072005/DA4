package dto;

/**
 * DTO for user registration request
 * Matches client-side RegisterRequest
 */
public class RegisterRequest {
    private String email;
    private String username;
    private String password; // SHA-256 hashed from client
    
    // Constructors
    public RegisterRequest() {
    }
    
    public RegisterRequest(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    @Override
    public String toString() {
        return "RegisterRequest{" +
                "email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}
