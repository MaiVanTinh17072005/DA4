package com.example.config;

/**
 * P2P Configuration
 * Cấu hình cho hệ thống P2P networking
 */
public class P2PConfig {
    
    // Port Configuration
    public static final int PORT_RANGE_START = 50000;
    public static final int PORT_RANGE_END = 60000;
    public static final int MAX_PORT_ALLOCATION_ATTEMPTS = 100;
    
    // TCP Configuration
    public static final int TCP_BUFFER_SIZE = 8192; // 8KB
    public static final int TCP_BACKLOG = 50; // Max pending connections
    public static final int TCP_SO_TIMEOUT = 30000; // 30 seconds
    
    // UDP Configuration
    public static final int UDP_BUFFER_SIZE = 512; // 512 bytes
    public static final int UDP_SO_TIMEOUT = 5000; // 5 seconds
    
    // Reconnection Configuration
    public static final int RECONNECT_DELAY_MS = 5000; // 5 seconds
    public static final int MAX_RECONNECT_ATTEMPTS = 10;
    
    // Heartbeat Configuration
    public static final int HEARTBEAT_INTERVAL_MS = 30000; // 30 seconds
    public static final int HEARTBEAT_TIMEOUT_MS = 90000; // 90 seconds (3x interval)
    
    // LAN Broadcast Configuration
    public static final int BROADCAST_PORT = 50999;
    public static final String BROADCAST_MESSAGE_PREFIX = "DISCORD_MINI_P2P";
    
    // Thread Configuration
    public static final String THREAD_NAME_PREFIX_TCP = "P2P-TCP-";
    public static final String THREAD_NAME_PREFIX_UDP = "P2P-UDP-";
    
    // Logging Configuration
    public static final String LOG_FILE_NAME = "p2p.log";
    public static final boolean ENABLE_CONSOLE_LOG = true;
    public static final boolean ENABLE_FILE_LOG = true;
    
    private P2PConfig() {
        // Private constructor to prevent instantiation
    }
}
