package com.example.network.model;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;

/**
 * Peer Session Information
 * Stores encryption keys and connection details for each peer
 */
public class PeerSession {
    
    private Long peerId;
    private String peerUsername;
    private String ipAddress;
    private int tcpPort;
    private int udpPort;
    
    // Encryption keys
    private SecretKey aesSessionKey;           // AES key for this session
    private PublicKey peerPublicKey;           // Peer's RSA public key
    private KeyPair myKeyPair;                 // My RSA key pair
    
    // Connection state
    private ConnectionState state;
    private long lastHeartbeat;
    private long connectedAt;
    
    // Message tracking
    private int messagesSent;
    private int messagesReceived;
    private String lastMessageId;
    
    public PeerSession(Long peerId, String ipAddress, int tcpPort, int udpPort) {
        this.peerId = peerId;
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.state = ConnectionState.DISCONNECTED;
        this.lastHeartbeat = System.currentTimeMillis();
        this.messagesSent = 0;
        this.messagesReceived = 0;
    }
    
    // Getters and Setters
    public Long getPeerId() {
        return peerId;
    }
    
    public void setPeerId(Long peerId) {
        this.peerId = peerId;
    }
    
    public String getPeerUsername() {
        return peerUsername;
    }
    
    public void setPeerUsername(String peerUsername) {
        this.peerUsername = peerUsername;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public int getTcpPort() {
        return tcpPort;
    }
    
    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }
    
    public int getUdpPort() {
        return udpPort;
    }
    
    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }
    
    public SecretKey getAesSessionKey() {
        return aesSessionKey;
    }
    
    public void setAesSessionKey(SecretKey aesSessionKey) {
        this.aesSessionKey = aesSessionKey;
    }
    
    public PublicKey getPeerPublicKey() {
        return peerPublicKey;
    }
    
    public void setPeerPublicKey(PublicKey peerPublicKey) {
        this.peerPublicKey = peerPublicKey;
    }
    
    public KeyPair getMyKeyPair() {
        return myKeyPair;
    }
    
    public void setMyKeyPair(KeyPair myKeyPair) {
        this.myKeyPair = myKeyPair;
    }
    
    public ConnectionState getState() {
        return state;
    }
    
    public void setState(ConnectionState state) {
        this.state = state;
        if (state == ConnectionState.CONNECTED) {
            this.connectedAt = System.currentTimeMillis();
        }
    }
    
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
    
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
    
    public long getConnectedAt() {
        return connectedAt;
    }
    
    public int getMessagesSent() {
        return messagesSent;
    }
    
    public void incrementMessagesSent() {
        this.messagesSent++;
    }
    
    public int getMessagesReceived() {
        return messagesReceived;
    }
    
    public void incrementMessagesReceived() {
        this.messagesReceived++;
    }
    
    public String getLastMessageId() {
        return lastMessageId;
    }
    
    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }
    
    public boolean isConnected() {
        return state == ConnectionState.CONNECTED;
    }
    
    public boolean isHeartbeatAlive(long timeoutMs) {
        return (System.currentTimeMillis() - lastHeartbeat) < timeoutMs;
    }
    
    @Override
    public String toString() {
        return "PeerSession{" +
                "peerId=" + peerId +
                ", peerUsername='" + peerUsername + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", tcpPort=" + tcpPort +
                ", state=" + state +
                ", messagesSent=" + messagesSent +
                ", messagesReceived=" + messagesReceived +
                '}';
    }
}
