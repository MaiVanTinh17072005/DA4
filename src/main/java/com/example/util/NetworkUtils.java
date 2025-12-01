package com.example.util;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * Network Utilities
 * Các tiện ích cho network operations
 */
public class NetworkUtils {
    
    private static final Logger LOGGER = Logger.getLogger(NetworkUtils.class.getName());
    
    private NetworkUtils() {
        // Private constructor
    }
    
    /**
     * Lấy địa chỉ IP LAN của máy
     * @return Địa chỉ IP hoặc null nếu không tìm thấy
     */
    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // Bỏ qua interface không hoạt động hoặc loopback
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // Chỉ lấy IPv4 và không phải loopback
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        String ip = address.getHostAddress();
                        LOGGER.info("Đã tìm thấy IP LAN: " + ip);
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.severe("Lỗi khi lấy IP LAN: " + e.getMessage());
        }
        
        // Fallback
        try {
            String fallbackIP = InetAddress.getLocalHost().getHostAddress();
            LOGGER.warning("Sử dụng fallback IP: " + fallbackIP);
            return fallbackIP;
        } catch (UnknownHostException e) {
            LOGGER.severe("Không thể lấy IP: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Kiểm tra port có khả dụng không
     * @param port Port cần kiểm tra
     * @return true nếu khả dụng
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
     * Lấy địa chỉ broadcast của subnet
     * @return Địa chỉ broadcast hoặc null
     */
    public static String getSubnetBroadcastAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        return broadcast.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.severe("Lỗi khi lấy broadcast address: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Kiểm tra có thể kết nối đến host:port không
     * @param ip Địa chỉ IP
     * @param port Port
     * @return true nếu có thể kết nối
     */
    public static boolean isReachable(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 2000); // 2 seconds timeout
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Kiểm tra địa chỉ IP có hợp lệ không
     * @param ip Địa chỉ IP
     * @return true nếu hợp lệ
     */
    public static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
