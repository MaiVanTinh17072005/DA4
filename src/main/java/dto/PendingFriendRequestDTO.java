package dto;

/**
 * DTO for Pending Friend Request
 * Represents a friend request waiting for acceptance
 */
public class PendingFriendRequestDTO {
    
    private String requestId;
    private Long senderId;
    private String senderUsername;
    private String senderEmail;
    private String senderAvatarUrl;
    private String senderStatus;
    private String timestamp;
    private String message; // Optional message from sender
    
    public PendingFriendRequestDTO() {
    }
    
    public PendingFriendRequestDTO(String requestId, Long senderId, String senderUsername, 
                                   String senderEmail, String senderAvatarUrl, String senderStatus, 
                                   String timestamp) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderEmail = senderEmail;
        this.senderAvatarUrl = senderAvatarUrl;
        this.senderStatus = senderStatus;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Long getSenderId() {
        return senderId;
    }
    
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderUsername() {
        return senderUsername;
    }
    
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }
    
    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }
    
    public String getSenderStatus() {
        return senderStatus;
    }
    
    public void setSenderStatus(String senderStatus) {
        this.senderStatus = senderStatus;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "PendingFriendRequestDTO{" +
                "requestId='" + requestId + '\'' +
                ", senderId=" + senderId +
                ", senderUsername='" + senderUsername + '\'' +
                ", senderEmail='" + senderEmail + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
