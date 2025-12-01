package com.example.network;

import com.example.config.P2PConfig;
import com.example.util.NetworkUtils;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

/**
 * UDP Client
 * Client UDP để gửi signaling packets đến peers
 */
public class UdpClient {
    
    private static final Logger LOGGER = Logger.getLogger(UdpClient.class.getName());
    
    private DatagramSocket socket;
    private final int localPort;
    
    public UdpClient(int localPort) {
        this.localPort = localPort;
        try {
            socket = new DatagramSocket(localPort);
            LOGGER.info("UDP Client khởi tạo trên port " + localPort);
        } catch (SocketException e) {
            LOGGER.severe("Không thể khởi tạo UDP Client: " + e.getMessage());
        }
    }
    
    /**
     * Gửi signal đến peer
     * @param signal Signal message
     * @param peerIP IP của peer
     * @param peerPort Port của peer
     * @return true nếu gửi thành công
     */
    public boolean sendSignal(String signal, String peerIP, int peerPort) {
        if (socket == null || socket.isClosed()) {
            LOGGER.warning("UDP socket chưa được khởi tạo");
            return false;
        }
        
        try {
            byte[] data = signal.getBytes();
            if (data.length > P2PConfig.UDP_BUFFER_SIZE) {
                LOGGER.warning("Signal quá lớn: " + data.length + " bytes");
                return false;
            }
            
            InetAddress address = InetAddress.getByName(peerIP);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, peerPort);
            
            socket.send(packet);
            LOGGER.info("Đã gửi UDP signal đến " + peerIP + ":" + peerPort + " - " + signal);
            return true;
            
        } catch (IOException e) {
            LOGGER.warning("Lỗi khi gửi UDP signal: " + e.getMessage());
            // Fallback to LAN broadcast
            return broadcastSignal(signal);
        }
    }
    
    /**
     * Broadcast signal trong LAN
     * @param signal Signal message
     * @return true nếu broadcast thành công
     */
    public boolean broadcastSignal(String signal) {
        try {
            String broadcastAddress = NetworkUtils.getSubnetBroadcastAddress();
            if (broadcastAddress == null) {
                LOGGER.warning("Không thể lấy broadcast address");
                return false;
            }
            
            byte[] data = (P2PConfig.BROADCAST_MESSAGE_PREFIX + ":" + signal).getBytes();
            InetAddress address = InetAddress.getByName(broadcastAddress);
            DatagramPacket packet = new DatagramPacket(
                    data, 
                    data.length, 
                    address, 
                    P2PConfig.BROADCAST_PORT
            );
            
            socket.setBroadcast(true);
            socket.send(packet);
            socket.setBroadcast(false);
            
            LOGGER.info("Đã broadcast signal trong LAN: " + signal);
            return true;
            
        } catch (IOException e) {
            LOGGER.severe("Lỗi khi broadcast signal: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gửi heartbeat đến peer
     * @param userId ID của user hiện tại
     * @param peerIP IP của peer
     * @param peerPort Port của peer
     * @return true nếu gửi thành công
     */
    public boolean sendHeartbeat(Long userId, String peerIP, int peerPort) {
        String signal = "HEARTBEAT:" + userId;
        return sendSignal(signal, peerIP, peerPort);
    }
    
    /**
     * Gửi typing indicator đến peer
     * @param userId ID của user hiện tại
     * @param peerIP IP của peer
     * @param peerPort Port của peer
     * @return true nếu gửi thành công
     */
    public boolean sendTyping(Long userId, String peerIP, int peerPort) {
        String signal = "TYPING:" + userId;
        return sendSignal(signal, peerIP, peerPort);
    }
    
    /**
     * Gửi online status đến peer
     * @param userId ID của user hiện tại
     * @param peerIP IP của peer
     * @param peerPort Port của peer
     * @return true nếu gửi thành công
     */
    public boolean sendOnline(Long userId, String peerIP, int peerPort) {
        String signal = "ONLINE:" + userId;
        return sendSignal(signal, peerIP, peerPort);
    }
    
    /**
     * Gửi offline status đến peer
     * @param userId ID của user hiện tại
     * @param peerIP IP của peer
     * @param peerPort Port của peer
     * @return true nếu gửi thành công
     */
    public boolean sendOffline(Long userId, String peerIP, int peerPort) {
        String signal = "OFFLINE:" + userId;
        return sendSignal(signal, peerIP, peerPort);
    }
    
    /**
     * Đóng UDP client
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            LOGGER.info("UDP Client đã đóng");
        }
    }
}
