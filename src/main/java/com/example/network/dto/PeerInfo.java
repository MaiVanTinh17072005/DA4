package com.example.network.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Peer Information DTO
 * Chứa thông tin về một peer trong mạng P2P
 */
public class PeerInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private String username;
    private String ipAddress;
    private Integer tcpPort;
    private Integer udpPort;
    private boolean online;
    private LocalDateTime lastHeartbeat;
    
    public PeerInfo() {
    }
    
    public PeerInfo(Long userId, String username, String ipAddress, Integer tcpPort, Integer udpPort) {
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.online = true;
        this.lastHeartbeat = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public Integer getTcpPort() {
        return tcpPort;
    }
    
    public void setTcpPort(Integer tcpPort) {
        this.tcpPort = tcpPort;
    }
    
    public Integer getUdpPort() {
        return udpPort;
    }
    
    public void setUdpPort(Integer udpPort) {
        this.udpPort = udpPort;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
    
    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
        this.online = true;
    }
    
    @Override
    public String toString() {
        return "PeerInfo{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                ", online=" + online +
                ", lastHeartbeat=" + lastHeartbeat +
                '}';
    }
}
