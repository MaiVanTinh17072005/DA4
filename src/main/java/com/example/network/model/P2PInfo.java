package com.example.network.model;

import java.io.Serializable;

/**
 * P2P Connection Information
 * Stores IP address and ports for P2P communication
 */
public class P2PInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private String ipAddress;
    private int tcpPort;
    private int udpPort;
    private String status;          // "online", "offline"
    private long lastUpdated;
    
    public P2PInfo() {
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public P2PInfo(Long userId, String ipAddress, int tcpPort, int udpPort) {
        this();
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.status = "online";
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    @Override
    public String toString() {
        return "P2PInfo{" +
                "userId=" + userId +
                ", ipAddress='" + ipAddress + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                ", status='" + status + '\'' +
                '}';
    }
}
