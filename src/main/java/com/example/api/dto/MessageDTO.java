package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Message Data Transfer Object (Frontend)
 * Mirrors backend MessageDTO for API communication
 */
public class MessageDTO {
    
    @JsonProperty("msgId")
    @SerializedName("msgId")
    private Long msgId;
    
    @JsonProperty("senderId")
    @SerializedName("senderId")
    private Long senderId;
    
    @JsonProperty("senderUsername")
    @SerializedName("senderUsername")
    private String senderUsername;
    
    @JsonProperty("receiverId")
    @SerializedName("receiverId")
    private Long receiverId;
    
    @JsonProperty("groupId")
    @SerializedName("groupId")
    private Long groupId;
    
    @JsonProperty("content")
    @SerializedName("content")
    private String content;
    
    @JsonProperty("msgType")
    @SerializedName("msgType")
    private String msgType;
    
    @JsonProperty("filePath")
    @SerializedName("filePath")
    private String filePath;
    
    @JsonProperty("timestamp")
    @SerializedName("timestamp")
    private String timestamp;
    
    @JsonProperty("isRead")
    @SerializedName("isRead")
    private Boolean isRead;
    
    @JsonProperty("aesEncrypted")
    @SerializedName("aesEncrypted")
    private Boolean aesEncrypted;
    
    @JsonProperty("iv")
    @SerializedName("iv")
    private String iv;
    
    @JsonProperty("authTag")
    @SerializedName("authTag")
    private String authTag;
    
    @JsonProperty("algorithm")
    @SerializedName("algorithm")
    private String algorithm;
    
    // Constructors
    public MessageDTO() {
    }
    
    // Getters and Setters
    public Long getMsgId() {
        return msgId;
    }
    
    public void setMsgId(Long msgId) {
        this.msgId = msgId;
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
    
    public Long getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getMsgType() {
        return msgType;
    }
    
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public Boolean getAesEncrypted() {
        return aesEncrypted;
    }
    
    public void setAesEncrypted(Boolean aesEncrypted) {
        this.aesEncrypted = aesEncrypted;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getAuthTag() {
        return authTag;
    }

    public void setAuthTag(String authTag) {
        this.authTag = authTag;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
