package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Message entity model
 * Represents messages sent between users or in groups
 */
@Entity
@Table(name = "message")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "msg_id")
    private Long msgId;
    
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    
    @Column(name = "receiver_id")
    private Long receiverId; // Nullable for group messages
    
    @Column(name = "group_id")
    private Long groupId; // Nullable for direct messages
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "msg_type", length = 20, nullable = false)
    private String msgType = "text"; // text, file, emoji, voice
    
    @Column(name = "file_path", length = 500)
    private String filePath;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "aes_encrypted", nullable = false)
    private Boolean aesEncrypted = false;
    
    // Constructors
    public Message() {
    }
    
    public Message(Long senderId, Long receiverId, Long groupId, String content, String msgType) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.content = content;
        this.msgType = msgType;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
        this.aesEncrypted = false;
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
        if (aesEncrypted == null) {
            aesEncrypted = false;
        }
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
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
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
        return "Message{" +
                "msgId=" + msgId +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", groupId=" + groupId +
                ", msgType='" + msgType + '\'' +
                ", timestamp=" + timestamp +
                ", isRead=" + isRead +
                '}';
    }
}
