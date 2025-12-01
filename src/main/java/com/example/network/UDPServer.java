package com.example.network;

import com.example.config.P2PConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * UDP Server
 * Server UDP để nhận signaling packets từ các peers
 */
public class UDPServer {
    
    private static final Logger LOGGER = Logger.getLogger(UDPServer.class.getName());
    
    private final int port;
    private DatagramSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread serverThread;
    private final UDPSignalHandler signalHandler;
    
    public UDPServer(int port, UDPSignalHandler signalHandler) {
        this.port = port;
        this.signalHandler = signalHandler;
    }
    
    /**
     * Khởi động UDP server
     */
    public void start() {
        if (running.get()) {
            LOGGER.warning("UDP Server đã đang chạy trên port " + port);
            return;
        }
        
        serverThread = Thread.ofVirtual()
                .name(P2PConfig.THREAD_NAME_PREFIX_UDP + "Server-" + port)
                .start(() -> {
                    try {
                        socket = new DatagramSocket(port);
                        socket.setSoTimeout(P2PConfig.UDP_SO_TIMEOUT);
                        running.set(true);
                        
                        LOGGER.info("UDP Server đang lắng nghe trên port " + port);
                        
                        byte[] buffer = new byte[P2PConfig.UDP_BUFFER_SIZE];
                        
                        while (running.get() && !Thread.currentThread().isInterrupted()) {
                            try {
                                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                                socket.receive(packet);
                                
                                handlePacket(packet);
                                
                            } catch (java.net.SocketTimeoutException e) {
                                // Timeout là bình thường - không có packet đến
                                // Không cần log để tránh làm rối console
                            } catch (SocketException e) {
                                if (running.get()) {
                                    LOGGER.warning("Socket đã đóng hoặc lỗi kết nối");
                                }
                            } catch (IOException e) {
                                if (running.get()) {
                                    LOGGER.warning("⚠ [UDP] Lỗi nhận packet: " + e.getMessage());
                                }
                            }
                        }
                    } catch (SocketException e) {
                        LOGGER.severe("Không thể khởi động UDP Server: " + e.getMessage());
                    } finally {
                        cleanup();
                    }
                });
    }
    
    /**
     * Xử lý UDP packet nhận được
     */
    private void handlePacket(DatagramPacket packet) {
        Thread.ofVirtual()
                .name(P2PConfig.THREAD_NAME_PREFIX_UDP + "Handler")
                .start(() -> {
                    try {
                        String message = new String(packet.getData(), 0, packet.getLength());
                        String senderIP = packet.getAddress().getHostAddress();
                        int senderPort = packet.getPort();
                        
                        LOGGER.info("Nhận UDP signal từ " + senderIP + ":" + senderPort + " - " + message);
                        
                        signalHandler.handleSignal(message, senderIP, senderPort);
                        
                    } catch (Exception e) {
                        LOGGER.severe("Lỗi khi xử lý UDP packet: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Dừng UDP server
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        LOGGER.info("Đang dừng UDP Server trên port " + port);
        running.set(false);
        
        cleanup();
        
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }
    
    /**
     * Dọn dẹp resources
     */
    private void cleanup() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            LOGGER.info("UDP Server đã đóng port " + port);
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public int getPort() {
        return port;
    }
}
