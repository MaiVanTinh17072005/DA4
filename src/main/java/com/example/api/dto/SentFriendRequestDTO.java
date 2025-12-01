package com.example.api.dto;

/**
 * DTO for Sent Friend Request (requests that current user has sent)
 */
public class SentFriendRequestDTO {
    
    private String requestId;
    private Long receiverId;
    private String receiverUsername;
    private String receiverEmail;
    private String receiverAvatarUrl;
    private String receiverStatus;
    private String timestamp;
    private String status; // "pending", "accepted", "rejected", "cancelled"
    
    public SentFriendRequestDTO() {
    }
    
    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Long getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getReceiverUsername() {
        return receiverUsername;
    }
    
    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }
    
    public String getReceiverEmail() {
        return receiverEmail;
    }
    
    public void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
    }
    
    public String getReceiverAvatarUrl() {
        return receiverAvatarUrl;
    }
    
    public void setReceiverAvatarUrl(String receiverAvatarUrl) {
        this.receiverAvatarUrl = receiverAvatarUrl;
    }
    
    public String getReceiverStatus() {
        return receiverStatus;
    }
    
    public void setReceiverStatus(String receiverStatus) {
        this.receiverStatus = receiverStatus;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "SentFriendRequestDTO{" +
                "requestId='" + requestId + '\'' +
                ", receiverId=" + receiverId +
                ", receiverUsername='" + receiverUsername + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
