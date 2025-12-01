package com.example.network;

import java.util.logging.Logger;

/**
 * UDP Signal Handler
 * Xử lý các signals nhận được qua UDP
 */
public class UDPSignalHandler {
    
    private static final Logger LOGGER = Logger.getLogger(UDPSignalHandler.class.getName());
    
    /**
     * Xử lý signal nhận được từ peer
     * @param signal Signal message
     * @param senderIP IP của sender
     * @param senderPort Port của sender
     */
    public void handleSignal(String signal, String senderIP, int senderPort) {
        try {
            LOGGER.info("Xử lý signal từ " + senderIP + ":" + senderPort + " - " + signal);
            
            // Parse signal type
            String[] parts = signal.split(":");
            if (parts.length < 2) {
                LOGGER.warning("Signal không hợp lệ: " + signal);
                return;
            }
            
            String signalType = parts[0];
            String payload = parts[1];
            
            switch (signalType) {
                case "HEARTBEAT":
                    handleHeartbeat(payload, senderIP, senderPort);
                    break;
                case "TYPING":
                    handleTyping(payload, senderIP, senderPort);
                    break;
                case "ONLINE":
                    handleOnline(payload, senderIP, senderPort);
                    break;
                case "OFFLINE":
                    handleOffline(payload, senderIP, senderPort);
                    break;
                default:
                    LOGGER.warning("Signal type không xác định: " + signalType);
            }
            
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý signal: " + e.getMessage());
        }
    }
    
    private void handleHeartbeat(String payload, String senderIP, int senderPort) {
        LOGGER.info("Heartbeat từ " + senderIP + " - User ID: " + payload);
        // TODO: Update peer's last heartbeat timestamp
    }
    
    private void handleTyping(String payload, String senderIP, int senderPort) {
        LOGGER.info("Typing indicator từ " + senderIP + " - User ID: " + payload);
        // TODO: Update UI to show typing indicator
    }
    
    private void handleOnline(String payload, String senderIP, int senderPort) {
        LOGGER.info("User online: " + payload + " từ " + senderIP);
        // TODO: Update peer status to online
    }
    
    private void handleOffline(String payload, String senderIP, int senderPort) {
        LOGGER.info("User offline: " + payload + " từ " + senderIP);
        // TODO: Update peer status to offline
    }
}
