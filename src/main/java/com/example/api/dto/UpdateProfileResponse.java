package com.example.api.dto;

/**
 * Response DTO for profile update operation
 * Contains success status, message, and updated user data
 */
public class UpdateProfileResponse {
    
    private boolean success;
    private String message;
    private UserDTO user;
    
    // Constructors
    public UpdateProfileResponse() {
    }
    
    public UpdateProfileResponse(boolean success, String message, UserDTO user) {
        this.success = success;
        this.message = message;
        this.user = user;
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
    
    public UserDTO getUser() {
        return user;
    }
    
    public void setUser(UserDTO user) {
        this.user = user;
    }
    
    @Override
    public String toString() {
        return "UpdateProfileResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", user=" + user +
                '}';
    }
}
