package dto;

/**
 * DTO for P2P information response
 * Sent back to client after registering P2P info
 */
public class P2PInfoResponse {
    
    private boolean success;
    private String message;
    
    public P2PInfoResponse() {
    }
    
    public P2PInfoResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Static factory methods
    public static P2PInfoResponse success(String message) {
        return new P2PInfoResponse(true, message);
    }
    
    public static P2PInfoResponse error(String message) {
        return new P2PInfoResponse(false, message);
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
        return "P2PInfoResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
