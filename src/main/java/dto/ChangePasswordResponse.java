package dto;

/**
 * Response DTO for change password operation
 * Sent to client with success status and message
 */
public class ChangePasswordResponse {
    
    private boolean success;
    private String message;
    
    // Constructors
    public ChangePasswordResponse() {
    }
    
    public ChangePasswordResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Static factory methods
    public static ChangePasswordResponse success(String message) {
        return new ChangePasswordResponse(true, message);
    }
    
    public static ChangePasswordResponse error(String message) {
        return new ChangePasswordResponse(false, message);
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
    
    @Override
    public String toString() {
        return "ChangePasswordResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
