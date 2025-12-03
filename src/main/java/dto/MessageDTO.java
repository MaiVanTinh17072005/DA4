package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message Data Transfer Object
 * Used for transferring message data between client and server
 */
public class MessageDTO {
    
    @JsonProperty("msgId")
    private Long msgId;
    
    @JsonProperty("senderId")
    private Long senderId;
    
    @JsonProperty("senderUsername")
    private String senderUsername;
    
    @JsonProperty("receiverId")
    private Long receiverId;
    
    @JsonProperty("groupId")
    private Long groupId;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("msgType")
    private String msgType; // text, file, emoji, voice
    
    @JsonProperty("filePath")
    private String filePath;
    
    @JsonProperty("timestamp")
    private String timestamp; // ISO format string
    
    @JsonProperty("isRead")
    private Boolean isRead;
    
    @JsonProperty("aesEncrypted")
    private Boolean aesEncrypted;
    
    // Constructors
    public MessageDTO() {
    }
    
    public MessageDTO(Long msgId, Long senderId, String senderUsername, Long receiverId, 
                     Long groupId, String content, String msgType, String filePath, 
                     String timestamp, Boolean isRead, Boolean aesEncrypted) {
        this.msgId = msgId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.content = content;
        this.msgType = msgType;
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.aesEncrypted = aesEncrypted;
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
    
    @Override
    public String toString() {
        return "MessageDTO{" +
                "msgId=" + msgId +
                ", senderId=" + senderId +
                ", senderUsername='" + senderUsername + '\'' +
                ", receiverId=" + receiverId +
                ", groupId=" + groupId +
                ", msgType='" + msgType + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}
