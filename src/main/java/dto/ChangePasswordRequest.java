package dto;

/**
 * Request DTO for changing password
 * Contains current password and new password (both SHA-256 hashed from client)
 */
public class ChangePasswordRequest {
    
    private String currentPassword;  // SHA-256 hashed
    private String newPassword;       // SHA-256 hashed
    
    // Constructors
    public ChangePasswordRequest() {
    }
    
    public ChangePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
    
    // Getters and Setters
    public String getCurrentPassword() {
        return currentPassword;
    }
    
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    
    @Override
    public String toString() {
        return "ChangePasswordRequest{" +
                "currentPassword='[PROTECTED]', " +
                "newPassword='[PROTECTED]'" +
                '}';
    }
}
