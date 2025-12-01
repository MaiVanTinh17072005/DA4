package dto;

/**
 * DTO for Friend Response
 * Used when returning friend request result
 */
public class FriendResponseDTO {
    
    private boolean success;
    private String message;
    
    public FriendResponseDTO() {
    }
    
    public FriendResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Static factory methods
    public static FriendResponseDTO success(String message) {
        return new FriendResponseDTO(true, message);
    }
    
    public static FriendResponseDTO error(String message) {
        return new FriendResponseDTO(false, message);
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
        return "FriendResponseDTO{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
