package dto;

/**
 * DTO for P2P information request
 * Sent from client to register P2P connection info
 */
public class P2PInfoRequest {
    
    private Long userId;
    private String ipAddress;
    private Integer tcpPort;
    private Integer udpPort;
    
    public P2PInfoRequest() {
    }
    
    public P2PInfoRequest(Long userId, String ipAddress, Integer tcpPort, Integer udpPort) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
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
    
    @Override
    public String toString() {
        return "P2PInfoRequest{" +
                "userId=" + userId +
                ", ipAddress='" + ipAddress + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                '}';
    }
}
