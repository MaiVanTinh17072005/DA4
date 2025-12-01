package com.example.api.dto;

/**
 * DTO for Pending Friend Request
 * Matches backend PendingFriendRequestDTO
 */
public class PendingFriendRequestDTO {
    
    private String requestId;
    private Long senderId;
    private String senderUsername;
    private String senderEmail;
    private String senderAvatarUrl;
    private String senderStatus;
    private String timestamp;
    private String message;
    
    public PendingFriendRequestDTO() {
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
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
