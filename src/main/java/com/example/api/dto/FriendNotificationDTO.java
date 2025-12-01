package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Friend Request Notification (client-side)
 */
public class FriendNotificationDTO {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("type")
    private String type; // "FRIEND_REQUEST_ACCEPTED", "FRIEND_REQUEST_REJECTED"
    
    @JsonProperty("fromUserId")
    private Long fromUserId;
    
    @JsonProperty("fromUsername")
    private String fromUsername;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("read")
    private boolean read;
    
    public FriendNotificationDTO() {
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getFromUserId() {
        return fromUserId;
    }
    
    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }
    
    public String getFromUsername() {
        return fromUsername;
    }
    
    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    @Override
    public String toString() {
        return "FriendNotificationDTO{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", fromUsername='" + fromUsername + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
