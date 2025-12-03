package dto;

import java.io.Serializable;

/**
 * P2P Info DTO for Backend
 * Stores peer connection information
 */
public class P2PInfoDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private String ipAddress;
    private Integer tcpPort;
    private Integer udpPort;
    private String status;  // "online", "offline"
    private Long lastUpdated;
    
    public P2PInfoDTO() {
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public P2PInfoDTO(Long userId, String ipAddress, Integer tcpPort, Integer udpPort) {
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    @Override
    public String toString() {
        return "P2PInfoDTO{" +
                "userId=" + userId +
                ", ipAddress='" + ipAddress + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                ", status='" + status + '\'' +
                '}';
    }
}
