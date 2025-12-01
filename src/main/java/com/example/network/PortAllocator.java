package com.example.network;

import com.example.config.P2PConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Port Allocator
 * Quản lý phân bổ và giải phóng ports cho P2P connections
 */
public class PortAllocator {
    
    private static final Logger LOGGER = Logger.getLogger(PortAllocator.class.getName());
    
    // Thread-safe set để theo dõi ports đang sử dụng
    private static final Set<Integer> allocatedPorts = ConcurrentHashMap.newKeySet();
    
    private PortAllocator() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Phân bổ một port khả dụng trong dải cho phép
     * @return Port number nếu thành công, -1 nếu không tìm được port
     */
    public static synchronized int allocatePort() {
        int attempts = 0;
        int startPort = P2PConfig.PORT_RANGE_START;
        int endPort = P2PConfig.PORT_RANGE_END;
        
        while (attempts < P2PConfig.MAX_PORT_ALLOCATION_ATTEMPTS) {
            // Tạo port ngẫu nhiên trong dải
            int port = startPort + (int) (Math.random() * (endPort - startPort));
            
            // Kiểm tra port chưa được phân bổ và khả dụng
            if (!allocatedPorts.contains(port) && isPortAvailable(port)) {
                allocatedPorts.add(port);
                LOGGER.info("Đã phân bổ port: " + port);
                return port;
            }
            
            attempts++;
        }
        
        LOGGER.severe("Không thể phân bổ port sau " + attempts + " lần thử");
        return -1;
    }
    
    /**
     * Giải phóng một port đã được phân bổ
     * @param port Port cần giải phóng
     */
    public static synchronized void releasePort(int port) {
        if (allocatedPorts.remove(port)) {
            LOGGER.info("Đã giải phóng port: " + port);
        } else {
            LOGGER.warning("Cố gắng giải phóng port chưa được phân bổ: " + port);
        }
    }
    
    /**
     * Kiểm tra port có khả dụng không
     * @param port Port cần kiểm tra
     * @return true nếu port khả dụng, false nếu không
     */
    public static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Lấy danh sách các ports đang được phân bổ
     * @return Set các ports đang sử dụng
     */
    public static Set<Integer> getAllocatedPorts() {
        return Set.copyOf(allocatedPorts);
    }
    
    /**
     * Giải phóng tất cả ports đã phân bổ
     */
    public static synchronized void releaseAllPorts() {
        LOGGER.info("Giải phóng tất cả " + allocatedPorts.size() + " ports");
        allocatedPorts.clear();
    }
}
