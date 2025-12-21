package com.example.network.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * P2P Message Protocol
 * Represents a message sent between peers via TCP socket
 */
public class P2PMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Message metadata
    private String messageId;
    private Long senderId;
    private Long receiverId;
    private String senderUsername;
    private long timestamp;
    
    // Message content
    private String msgType;          // "text", "file", "signal", "ack", "typing"
    private String content;           // Plain text content OR encrypted content (Base64)
    private byte[] encryptedData;     // AES encrypted content (binary)
    
    // E2EE fields (for True E2EE)
    private String iv;                // Initialization Vector (Base64)
    private String authTag;           // Authentication Tag (Base64)
    private String algorithm;         // Encryption algorithm (e.g., "AES-256-GCM")
    private boolean isEncrypted;      // Whether content is encrypted
    
    // Security
    private byte[] signature;         // Message integrity signature
    private byte[] sessionKeyEncrypted; // RSA encrypted AES session key (for first message)
    
    // Message state
    private boolean requiresAck;      // Whether this message needs acknowledgment
    private String ackForMessageId;   // If this is an ACK, which message it's for
    
    // Constructors
    public P2PMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.requiresAck = true;
    }
    
    public P2PMessage(Long senderId, Long receiverId, String msgType, String content) {
        this();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.msgType = msgType;
        this.content = content;
    }
    
    // Static factory methods
    public static P2PMessage createTextMessage(Long senderId, Long receiverId, String content) {
        return new P2PMessage(senderId, receiverId, "text", content);
    }
    
    public static P2PMessage createAck(String originalMessageId, Long senderId, Long receiverId) {
        P2PMessage ack = new P2PMessage(senderId, receiverId, "ack", "");
        ack.setAckForMessageId(originalMessageId);
        ack.setRequiresAck(false);
        return ack;
    }
    
    public static P2PMessage createTypingIndicator(Long senderId, Long receiverId, boolean isTyping) {
        P2PMessage msg = new P2PMessage(senderId, receiverId, "typing", isTyping ? "1" : "0");
        msg.setRequiresAck(false);
        return msg;
    }
    
    public static P2PMessage createSignal(Long senderId, Long receiverId, String signalType) {
        P2PMessage msg = new P2PMessage(senderId, receiverId, "signal", signalType);
        msg.setRequiresAck(false);
        return msg;
    }
    
    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
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
    
    public String getSenderUsername() {
        return senderUsername;
    }
    
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMsgType() {
        return msgType;
    }
    
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public byte[] getEncryptedData() {
        return encryptedData;
    }
    
    public void setEncryptedData(byte[] encryptedData) {
        this.encryptedData = encryptedData;
    }
    
    public byte[] getSignature() {
        return signature;
    }
    
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
    
    public byte[] getSessionKeyEncrypted() {
        return sessionKeyEncrypted;
    }
    
    public void setSessionKeyEncrypted(byte[] sessionKeyEncrypted) {
        this.sessionKeyEncrypted = sessionKeyEncrypted;
    }
    
    public boolean isRequiresAck() {
        return requiresAck;
    }
    
    public void setRequiresAck(boolean requiresAck) {
        this.requiresAck = requiresAck;
    }
    
    public String getAckForMessageId() {
        return ackForMessageId;
    }
    
    public void setAckForMessageId(String ackForMessageId) {
        this.ackForMessageId = ackForMessageId;
    }
    
    // E2EE Getters and Setters
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
    
    public boolean isEncrypted() {
        return isEncrypted;
    }
    
    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }
    
    @Override
    public String toString() {
        return "P2PMessage{" +
                "messageId='" + messageId + '\'' +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", msgType='" + msgType + '\'' +
                ", timestamp=" + timestamp +
                ", requiresAck=" + requiresAck +
                '}';
    }
}
