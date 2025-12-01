package com.example.network;

import java.io.ObjectOutputStream;
import java.util.logging.Logger;

/**
 * TCP Message Handler
 * Xử lý các messages nhận được qua TCP
 */
public class TCPMessageHandler {
    
    private static final Logger LOGGER = Logger.getLogger(TCPMessageHandler.class.getName());
    
    /**
     * Xử lý message nhận được từ peer
     * @param message Object message nhận được
     * @param out Output stream để gửi response
     */
    public void handleMessage(Object message, ObjectOutputStream out) {
        try {
            LOGGER.info("Nhận message: " + message.getClass().getSimpleName());
            
            // TODO: Implement message routing based on message type
            // Ví dụ:
            // if (message instanceof ChatMessage) {
            //     handleChatMessage((ChatMessage) message);
            // } else if (message instanceof FileTransfer) {
            //     handleFileTransfer((FileTransfer) message);
            // } else if (message instanceof VoiceChunk) {
            //     handleVoiceChunk((VoiceChunk) message);
            // }
            
            // Tạm thời log message
            LOGGER.info("Message content: " + message.toString());
            
            // Gửi acknowledgment
            out.writeObject("ACK");
            out.flush();
            
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý message: " + e.getMessage());
        }
    }
    
    // TODO: Implement specific message handlers
    // private void handleChatMessage(ChatMessage message) { }
    // private void handleFileTransfer(FileTransfer message) { }
    // private void handleVoiceChunk(VoiceChunk message) { }
}
