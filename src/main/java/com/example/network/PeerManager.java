package com.example.network;

import com.example.config.P2PConfig;
import com.example.network.dto.PeerInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Peer Manager
 * Quản lý trung tâm cho tất cả P2P connections
 */
public class PeerManager {
    
    private static final Logger LOGGER = Logger.getLogger(PeerManager.class.getName());
    private static PeerManager instance;
    
    private TCPServer tcpServer;
    private UDPServer udpServer;
    private UdpClient udpClient;
    
    private int tcpPort;
    private int udpPort;
    
    // Map userId -> TcpClient
    private final Map<Long, TcpClient> peerConnections = new ConcurrentHashMap<>();
    
    // Map userId -> PeerInfo
    private final Map<Long, PeerInfo> peers = new ConcurrentHashMap<>();
    
    // Heartbeat scheduler
    private ScheduledExecutorService heartbeatScheduler;
    
    private PeerManager() {
    }
    
    public static synchronized PeerManager getInstance() {
        if (instance == null) {
            instance = new PeerManager();
        }
        return instance;
    }
    
    /**
     * Khởi động P2P servers
     * @param tcpPort TCP port
     * @param udpPort UDP port
     */
    public void startP2P(int tcpPort, int udpPort) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        
        LOGGER.info("Đang khởi động P2P - TCP:" + tcpPort + " UDP:" + udpPort);
        
        // Khởi tạo handlers
        TCPMessageHandler tcpHandler = new TCPMessageHandler();
        UDPSignalHandler udpHandler = new UDPSignalHandler();
        
        // Khởi động servers
        tcpServer = new TCPServer(tcpPort, tcpHandler);
        tcpServer.start();
        
        udpServer = new UDPServer(udpPort, udpHandler);
        udpServer.start();
        
        // Khởi tạo UDP client
        udpClient = new UdpClient(udpPort);
        
        // Bắt đầu heartbeat scheduler
        startHeartbeatScheduler();
        
        LOGGER.info("P2P đã khởi động thành công");
    }
    
    /**
     * Dừng P2P servers
     */
    public void stopP2P() {
        LOGGER.info("Đang dừng P2P...");
        
        // Dừng heartbeat
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
        }
        
        // Ngắt tất cả peer connections
        peerConnections.values().forEach(TcpClient::disconnect);
        peerConnections.clear();
        peers.clear();
        
        // Dừng servers
        if (tcpServer != null) {
            tcpServer.stop();
        }
        if (udpServer != null) {
            udpServer.stop();
        }
        if (udpClient != null) {
            udpClient.close();
        }
        
        // Giải phóng ports
        PortAllocator.releasePort(tcpPort);
        PortAllocator.releasePort(udpPort);
        
        LOGGER.info("P2P đã dừng");
    }
    
    /**
     * Kết nối đến một peer
     * @param peer Thông tin peer
     * @return true nếu kết nối thành công
     */
    public boolean connectToPeer(PeerInfo peer) {
        if (peerConnections.containsKey(peer.getUserId())) {
            LOGGER.info("Đã kết nối đến peer " + peer.getUserId());
            return true;
        }
        
        TcpClient client = new TcpClient(peer.getIpAddress(), peer.getTcpPort());
        if (client.connect()) {
            peerConnections.put(peer.getUserId(), client);
            peers.put(peer.getUserId(), peer);
            LOGGER.info("Đã kết nối đến peer: " + peer);
            return true;
        }
        
        return false;
    }
    
    /**
     * Ngắt kết nối khỏi peer
     * @param userId ID của peer
     */
    public void disconnectPeer(Long userId) {
        TcpClient client = peerConnections.remove(userId);
        if (client != null) {
            client.disconnect();
            peers.remove(userId);
            LOGGER.info("Đã ngắt kết nối khỏi peer " + userId);
        }
    }
    
    /**
     * Gửi message đến peer qua TCP
     * @param userId ID của peer
     * @param message Message cần gửi
     * @return true nếu gửi thành công
     */
    public boolean sendMessage(Long userId, Object message) {
        TcpClient client = peerConnections.get(userId);
        if (client == null) {
            LOGGER.warning("Chưa kết nối đến peer " + userId);
            return false;
        }
        
        return client.sendMessage(message);
    }
    
    /**
     * Gửi signal đến peer qua UDP
     * @param userId ID của peer
     * @param signal Signal message
     * @return true nếu gửi thành công
     */
    public boolean sendSignal(Long userId, String signal) {
        PeerInfo peer = peers.get(userId);
        if (peer == null) {
            LOGGER.warning("Không tìm thấy thông tin peer " + userId);
            return false;
        }
        
        return udpClient.sendSignal(signal, peer.getIpAddress(), peer.getUdpPort());
    }
    
    /**
     * Bắt đầu heartbeat scheduler
     */
    private void startHeartbeatScheduler() {
        heartbeatScheduler = Executors.newScheduledThreadPool(1);
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeats,
                0,
                P2PConfig.HEARTBEAT_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        LOGGER.info("Heartbeat scheduler đã khởi động");
    }
    
    /**
     * Gửi heartbeat đến tất cả peers
     */
    private void sendHeartbeats() {
        // TODO: Get current user ID from SessionManager
        Long currentUserId = 1L; // Placeholder
        
        peers.values().forEach(peer -> {
            udpClient.sendHeartbeat(currentUserId, peer.getIpAddress(), peer.getUdpPort());
        });
    }
    
    public Map<Long, PeerInfo> getPeers() {
        return Map.copyOf(peers);
    }
    
    public int getTcpPort() {
        return tcpPort;
    }
    
    public int getUdpPort() {
        return udpPort;
    }
}
