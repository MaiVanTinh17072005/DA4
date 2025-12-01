package com.example.network;

import com.example.config.P2PConfig;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * TCP Client
 * Client TCP để kết nối và gửi messages đến peers
 */
public class TcpClient {
    
    private static final Logger LOGGER = Logger.getLogger(TcpClient.class.getName());
    
    private final String peerIP;
    private final int peerPort;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Thread reconnectThread;
    
    public TcpClient(String peerIP, int peerPort) {
        this.peerIP = peerIP;
        this.peerPort = peerPort;
    }
    
    /**
     * Kết nối đến peer
     * @return true nếu kết nối thành công
     */
    public boolean connect() {
        try {
            socket = new Socket(peerIP, peerPort);
            socket.setSoTimeout(P2PConfig.TCP_SO_TIMEOUT);
            
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            
            connected.set(true);
            LOGGER.info("Đã kết nối đến peer " + peerIP + ":" + peerPort);
            
            return true;
        } catch (IOException e) {
            LOGGER.warning("Không thể kết nối đến " + peerIP + ":" + peerPort + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gửi message đến peer
     * @param message Object cần gửi
     * @return true nếu gửi thành công
     */
    public boolean sendMessage(Object message) {
        if (!connected.get()) {
            LOGGER.warning("Chưa kết nối đến peer");
            return false;
        }
        
        try {
            out.writeObject(message);
            out.flush();
            LOGGER.info("Đã gửi message đến " + peerIP + ":" + peerPort);
            return true;
        } catch (IOException e) {
            LOGGER.severe("Lỗi khi gửi message: " + e.getMessage());
            handleConnectionLoss();
            return false;
        }
    }
    
    /**
     * Nhận message từ peer
     * @return Object nhận được hoặc null
     */
    public Object receiveMessage() {
        if (!connected.get()) {
            return null;
        }
        
        try {
            return in.readObject();
        } catch (EOFException e) {
            LOGGER.info("Peer đã đóng kết nối");
            handleConnectionLoss();
            return null;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.warning("Lỗi khi nhận message: " + e.getMessage());
            handleConnectionLoss();
            return null;
        }
    }
    
    /**
     * Xử lý khi mất kết nối
     */
    private void handleConnectionLoss() {
        connected.set(false);
        disconnect();
        attemptReconnect();
    }
    
    /**
     * Thử reconnect tự động
     */
    private void attemptReconnect() {
        if (reconnectThread != null && reconnectThread.isAlive()) {
            return; // Đã có thread reconnect đang chạy
        }
        
        reconnectThread = Thread.ofVirtual()
                .name(P2PConfig.THREAD_NAME_PREFIX_TCP + "Reconnect-" + peerIP)
                .start(() -> {
                    int attempts = 0;
                    while (attempts < P2PConfig.MAX_RECONNECT_ATTEMPTS && !connected.get()) {
                        try {
                            LOGGER.info("Thử reconnect lần " + (attempts + 1) + " đến " + peerIP);
                            Thread.sleep(P2PConfig.RECONNECT_DELAY_MS);
                            
                            if (connect()) {
                                LOGGER.info("Reconnect thành công đến " + peerIP);
                                return;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        attempts++;
                    }
                    LOGGER.warning("Không thể reconnect sau " + attempts + " lần thử");
                });
    }
    
    /**
     * Ngắt kết nối
     */
    public void disconnect() {
        connected.set(false);
        
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            LOGGER.info("Đã ngắt kết nối khỏi " + peerIP + ":" + peerPort);
        } catch (IOException e) {
            LOGGER.warning("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }
    
    public String getPeerIP() {
        return peerIP;
    }
    
    public int getPeerPort() {
        return peerPort;
    }
}
