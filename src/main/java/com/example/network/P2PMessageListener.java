package com.example.network;

import com.example.network.model.P2PMessage;

/**
 * P2P Message Listener Interface
 * Implement this to receive P2P messages
 */
public interface P2PMessageListener {
    
    /**
     * Called when a new message is received from a peer
     */
    void onMessageReceived(P2PMessage message);
    
    /**
     * Called when a peer connects
     */
    void onPeerConnected(Long peerId, String peerUsername);
    
    /**
     * Called when a peer disconnects
     */
    void onPeerDisconnected(Long peerId);
    
    /**
     * Called when a connection error occurs
     */
    void onConnectionError(Long peerId, Exception e);
    
    /**
     * Called when typing indicator is received
     */
    void onTypingIndicator(Long peerId, boolean isTyping);
    
    /**
     * Called when message acknowledgment is received
     */
    void onMessageAcknowledged(String messageId);
}
