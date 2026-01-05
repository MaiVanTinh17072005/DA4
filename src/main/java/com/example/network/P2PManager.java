package com.example.network;

import com.example.api.dto.UserDTO;
import com.example.crypto.P2PEncryption;
import com.example.network.model.P2PMessage;
import com.example.network.model.PeerSession;
import com.example.util.SessionManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * P2P Manager - Facade for P2P Communication
 * Combines TCP connection, UDP heartbeat, and message queue
 */
public class P2PManager implements P2PMessageListener, UDPHeartbeatService.HeartbeatListener {

    // Singleton instance
    private static P2PManager instance;

    private Long myUserId;
    private int tcpPort;
    private int udpPort;

    // Core components
    private P2PConnectionManager connectionManager;
    private UDPHeartbeatService heartbeatService;
    private MessageQueue messageQueue;

    // External listener (e.g., ChatScene)
    private P2PMessageListener externalListener;

    // State
    private boolean initialized = false;

    private P2PManager() {
        // Private constructor for singleton
    }

    /**
     * Get singleton instance
     */
    public static synchronized P2PManager getInstance() {
        if (instance == null) {
            instance = new P2PManager();
        }
        return instance;
    }

    /**
     * Initialize with user info and ports
     * Can be called multiple times, will only initialize once
     */
    public void initializeForUser(Long userId, int tcpPort, int udpPort) throws Exception {
        if (initialized) {
            System.out.println("[P2PManager] Already initialized for user: " + this.myUserId);
            return;
        }

        this.myUserId = userId;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;

        initialize();
    }

    /**
     * Initialize P2P system
     */
    public void initialize() throws Exception {
        if (initialized) {
            System.out.println("[P2PManager] Already initialized");
            return;
        }

        System.out.println("[P2PManager] ===== INITIALIZING P2P SYSTEM =====");
        System.out.println("[P2PManager] User ID: " + myUserId);
        System.out.println("[P2PManager] TCP Port: " + tcpPort);
        System.out.println("[P2PManager] UDP Port: " + udpPort);

        // Initialize components
        connectionManager = new P2PConnectionManager(myUserId, tcpPort);
        connectionManager.addMessageListener(this);
        connectionManager.startListening();

        heartbeatService = new UDPHeartbeatService(myUserId, udpPort);
        heartbeatService.setListener(this);
        heartbeatService.start();

        // Initialize message queue
        String userHome = System.getProperty("user.home");
        String queueDir = userHome + "/.discord_mini/queue";
        messageQueue = new MessageQueue(myUserId, queueDir);
        messageQueue.loadAllQueues();

        initialized = true;

        System.out.println("[P2PManager] ✅ P2P System initialized successfully");
    }

    /**
     * Connect to a friend for P2P chat
     */
    public boolean connectToFriend(UserDTO friend, String ipAddress, int friendTcpPort, int friendUdpPort) {
        if (!initialized) {
            System.err.println("[P2PManager] ❌ P2P not initialized");
            return false;
        }

        System.out.println("[P2PManager] 🔗 Connecting to friend: " + friend.getUsername());

        // Connect TCP
        boolean connected = connectionManager.connectToPeer(friend.getId(), ipAddress, friendTcpPort);

        if (connected) {
            // Start heartbeat
            heartbeatService.startHeartbeat(friend.getId(), ipAddress, friendUdpPort);

            // Send queued messages
            sendQueuedMessages(friend.getId());

            System.out.println("[P2PManager] ✅ Connected to friend: " + friend.getUsername());
        }

        return connected;
    }

    /**
     * Send text message to friend
     */
    public CompletableFuture<Boolean> sendTextMessage(Long friendId, String content) {
        if (!initialized) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }

        P2PMessage message = P2PMessage.createTextMessage(myUserId, friendId, content);

        UserDTO currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            message.setSenderUsername(currentUser.getUsername());
        }

        // Check if connected
        if (connectionManager.isConnected(friendId)) {
            System.out.println("[P2PManager] 📤 Sending message to friend " + friendId);
            return connectionManager.sendMessage(friendId, message);
        } else {
            // Queue message
            System.out.println("[P2PManager] ⏳ Friend offline, queuing message");
            messageQueue.enqueue(friendId, message);

            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(true); // Queued successfully
            return future;
        }
    }

    /**
     * Send P2PMessage with full E2EE fields (IV, AuthTag, Algorithm)
     * This preserves encryption metadata during transmission
     */
    public CompletableFuture<Boolean> sendP2PMessage(Long friendId, P2PMessage message) {
        if (!initialized) {
            return CompletableFuture.completedFuture(false);
        }

        // Set sender username if not already set
        if (message.getSenderUsername() == null) {
            UserDTO currentUser = SessionManager.getCurrentUser();
            if (currentUser != null) {
                message.setSenderUsername(currentUser.getUsername());
            }
        }

        // Check if connected
        if (connectionManager.isConnected(friendId)) {
            System.out.println("[P2PManager] 📤 Sending P2PMessage to friend " + friendId);
            if (message.isEncrypted()) {
                System.out.println("[P2PManager] 🔐 Message is encrypted with E2EE fields");
            }
            return connectionManager.sendMessage(friendId, message);
        } else {
            // Queue message
            System.out.println("[P2PManager] ⏳ Friend offline, queuing P2PMessage");
            messageQueue.enqueue(friendId, message);
            return CompletableFuture.completedFuture(true); // Queued successfully
        }
    }

    /**
     * Send typing indicator
     */
    public void sendTypingIndicator(Long friendId, boolean isTyping) {
        if (!initialized || !connectionManager.isConnected(friendId)) {
            return;
        }

        P2PMessage message = P2PMessage.createTypingIndicator(myUserId, friendId, isTyping);
        connectionManager.sendMessage(friendId, message);
    }

    /**
     * Disconnect from friend
     */
    public void disconnectFromFriend(Long friendId) {
        if (!initialized) {
            return;
        }

        heartbeatService.stopHeartbeat(friendId);
        connectionManager.disconnect(friendId);

        System.out.println("[P2PManager] 🔌 Disconnected from friend: " + friendId);
    }

    /**
     * Check if connected to friend
     */
    public boolean isConnectedToFriend(Long friendId) {
        return initialized && connectionManager.isConnected(friendId);
    }

    /**
     * Get peer session info
     */
    public PeerSession getPeerSession(Long friendId) {
        if (!initialized) {
            return null;
        }
        return connectionManager.getPeerSession(friendId);
    }

    /**
     * Shutdown P2P system
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        System.out.println("[P2PManager] ===== SHUTTING DOWN P2P SYSTEM =====");

        if (heartbeatService != null) {
            heartbeatService.shutdown();
        }

        if (connectionManager != null) {
            connectionManager.shutdown();
        }

        initialized = false;

        System.out.println("[P2PManager] ✅ P2P System shutdown complete");
    }

    /**
     * Set external message listener (e.g., ChatScene)
     */
    public void setMessageListener(P2PMessageListener listener) {
        this.externalListener = listener;
    }

    /**
     * Get my RSA public key (for sharing with peers)
     */
    public String getMyPublicKey() {
        if (!initialized) {
            return null;
        }
        return P2PEncryption.publicKeyToString(connectionManager.getMyPublicKey());
    }

    
    /**
     * Get heartbeat service for direct access
     * Used by ChatScene to set up monitoring for all friends
     */
    public UDPHeartbeatService getHeartbeatService() {
        return heartbeatService;
    }
    
    /**
     * Broadcast ONLINE status to all connected peers
     * Call this after successful login
     */
    public void broadcastOnlineStatus() {
        if (!initialized) {
            System.err.println("[P2PManager] ❌ Cannot broadcast - P2P not initialized");
            return;
        }
        
        System.out.println("[P2PManager] 📢 Broadcasting ONLINE status to ALL friends");
        
        // First, broadcast to already monitored peers
        heartbeatService.broadcastOnlineToAll();
        
        // CRITICAL FIX: Also broadcast to ALL friends (not just monitored ones)
        // This ensures friends who logged in before us receive our ONLINE signal
        CompletableFuture.runAsync(() -> {
            try {
                // Fetch all friends from API
                com.example.service.FriendService friendService = new com.example.service.FriendService();
                java.util.List<com.example.api.dto.UserDTO> friends = friendService.getFriendsList();
                
                if (friends != null && !friends.isEmpty()) {
                    System.out.println("[P2PManager] 📡 Broadcasting to " + friends.size() + " friends from API");
                    
                    com.example.service.P2PService p2pService = com.example.service.P2PService.getInstance();
                    
                    for (com.example.api.dto.UserDTO friend : friends) {
                        try {
                            // Get friend's P2P info
                            com.example.api.dto.P2PInfoRequest peerInfo = p2pService.getPeerInfo(friend.getId());
                            
                            if (peerInfo != null) {
                                // Send ONLINE signal directly
                                heartbeatService.broadcastOnlineToPeer(
                                    friend.getId(),
                                    peerInfo.getIpAddress(),
                                    peerInfo.getUdpPort()
                                );
                            }
                        } catch (Exception e) {
                            // Friend offline or unavailable - skip
                            System.out.println("[P2PManager] ⚠️ Could not broadcast to friend " + friend.getId());
                        }
                    }
                    
                    System.out.println("[P2PManager] ✅ ONLINE broadcast complete");
                }
            } catch (Exception e) {
                System.err.println("[P2PManager] ❌ Error broadcasting to all friends: " + e.getMessage());
            }
        });
    }
    
    /**
     * Broadcast OFFLINE status to all connected peers
     * Call this before logout or app shutdown
     * IMPORTANT: This method is SYNCHRONOUS to ensure broadcast completes before shutdown
     */
    public void broadcastOfflineStatus() {
        if (!initialized) {
            System.err.println("[P2PManager] ❌ Cannot broadcast - P2P not initialized");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        System.out.println("[P2PManager] 📢 Broadcasting OFFLINE status to ALL friends (SYNCHRONOUS)");
        
        // First, broadcast to already monitored peers
        heartbeatService.broadcastOfflineToAll();
        
        // CRITICAL FIX: Also broadcast to ALL friends (not just monitored ones)
        // This ensures friends who are online receive our OFFLINE signal
        int broadcastCount = 0;
        try {
            // Fetch all friends from API
            com.example.service.FriendService friendService = new com.example.service.FriendService();
            java.util.List<com.example.api.dto.UserDTO> friends = friendService.getFriendsList();
            
            if (friends != null && !friends.isEmpty()) {
                System.out.println("[P2PManager] 📡 Broadcasting OFFLINE to " + friends.size() + " friends from API");
                
                com.example.service.P2PService p2pService = com.example.service.P2PService.getInstance();
                
                for (com.example.api.dto.UserDTO friend : friends) {
                    try {
                        // Get friend's P2P info
                        com.example.api.dto.P2PInfoRequest peerInfo = p2pService.getPeerInfo(friend.getId());
                        
                        if (peerInfo != null) {
                            // Send OFFLINE signal directly
                            heartbeatService.broadcastOfflineToPeer(
                                friend.getId(),
                                peerInfo.getIpAddress(),
                                peerInfo.getUdpPort()
                            );
                            broadcastCount++;
                        }
                    } catch (Exception e) {
                        // Friend offline or unavailable - skip
                        System.out.println("[P2PManager] ⚠️ Could not broadcast OFFLINE to friend " + friend.getId());
                    }
                }
                
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("[P2PManager] ✅ OFFLINE broadcast complete - sent to " + broadcastCount + " friends in " + elapsed + "ms");
            }
        } catch (Exception e) {
            System.err.println("[P2PManager] ❌ Error broadcasting OFFLINE to all friends: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Give UDP packets time to send before shutdown
        // CRITICAL: Must wait long enough for all packets to be sent
        try {
            System.out.println("[P2PManager] ⏳ Waiting 500ms for UDP packets to be sent...");
            Thread.sleep(500);  // Increased from 200ms to 500ms
            System.out.println("[P2PManager] ✅ Broadcast complete, safe to shutdown");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ===== P2PMessageListener Implementation =====

    @Override
    public void onMessageReceived(P2PMessage message) {
        System.out.println("[P2PManager] 📨 Message received from peer: " + message.getSenderId());

        // Forward to external listener
        if (externalListener != null) {
            externalListener.onMessageReceived(message);
        }
    }

    @Override
    public void onPeerConnected(Long peerId, String peerUsername) {
        System.out.println("[P2PManager] ✅ Peer connected: " + peerId);

        // Send queued messages
        sendQueuedMessages(peerId);

        // IMPORTANT: Start UDP heartbeat for bidirectional status monitoring
        // When peer connects to us, we need to monitor their status too
        if (!heartbeatService.isMonitoring(peerId)) {
            System.out.println("[P2PManager] 🔄 Starting UDP heartbeat for bidirectional status...");

            // Fetch peer's P2P info to get UDP port
            CompletableFuture.runAsync(() -> {
                try {
                    com.example.service.P2PService p2pService = com.example.service.P2PService.getInstance();
                    com.example.api.dto.P2PInfoRequest peerInfo = p2pService.getPeerInfo(peerId);

                    if (peerInfo != null) {
                        System.out.println("[P2PManager] 📡 Starting heartbeat monitoring for peer: " + peerId);

                        // Start UDP heartbeat monitoring (no TCP connection needed)
                        heartbeatService.startMonitoring(
                                peerId,
                                peerInfo.getIpAddress(),
                                peerInfo.getUdpPort());

                        System.out.println("[P2PManager] ✅ Bidirectional heartbeat established with peer: " + peerId);
                    }
                } catch (Exception e) {
                    System.err.println("[P2PManager] ⚠️ Failed to start heartbeat monitoring: " + e.getMessage());
                }
            });
        }

        // Forward to external listener
        if (externalListener != null) {
            externalListener.onPeerConnected(peerId, peerUsername);
        }
    }

    @Override
    public void onPeerDisconnected(Long peerId) {
        System.out.println("[P2PManager] 🔌 Peer disconnected: " + peerId);

        // Stop heartbeat
        heartbeatService.stopHeartbeat(peerId);

        // Forward to external listener
        if (externalListener != null) {
            externalListener.onPeerDisconnected(peerId);
        }
    }

    @Override
    public void onConnectionError(Long peerId, Exception e) {
        System.err.println("[P2PManager] ❌ Connection error with peer " + peerId + ": " + e.getMessage());

        // Forward to external listener
        if (externalListener != null) {
            externalListener.onConnectionError(peerId, e);
        }
    }

    @Override
    public void onTypingIndicator(Long peerId, boolean isTyping) {
        // Forward to external listener
        if (externalListener != null) {
            externalListener.onTypingIndicator(peerId, isTyping);
        }
    }

    @Override
    public void onMessageAcknowledged(String messageId) {
        System.out.println("[P2PManager] ✅ Message acknowledged: " + messageId);

        // Forward to external listener
        if (externalListener != null) {
            externalListener.onMessageAcknowledged(messageId);
        }
    }

    // ===== HeartbeatListener Implementation =====

    @Override
    public void onHeartbeatReceived(Long peerId) {
        // Heartbeat received, peer is alive
        PeerSession session = connectionManager.getPeerSession(peerId);
        if (session != null) {
            session.updateHeartbeat();
        }
    }

    @Override
    public void onPeerTimeout(Long peerId) {
        System.out.println("[P2PManager] ⏱️ Peer timeout: " + peerId);

        // Try to reconnect
        PeerSession session = connectionManager.getPeerSession(peerId);
        if (session != null) {
            System.out.println("[P2PManager] 🔄 Attempting to reconnect to peer: " + peerId);
            connectionManager.connectToPeer(peerId, session.getIpAddress(), session.getTcpPort());
        }
    }

    
    @Override
    public void onPeerOnline(Long peerId) {
        System.out.println("[P2PManager] 🟢 Peer came online: " + peerId);
        
        // CRITICAL FIX: If we're not monitoring this peer yet, start monitoring now!
        // This handles the case where friend B logs in after friend A
        if (!heartbeatService.isMonitoring(peerId)) {
            System.out.println("[P2PManager] 🔄 Not monitoring peer " + peerId + " yet, fetching P2P info to start monitoring...");
            
            // Fetch peer's P2P info and start monitoring
            CompletableFuture.runAsync(() -> {
                try {
                    com.example.service.P2PService p2pService = com.example.service.P2PService.getInstance();
                    com.example.api.dto.P2PInfoRequest peerInfo = p2pService.getPeerInfo(peerId);
                    
                    if (peerInfo != null) {
                        System.out.println("[P2PManager] 📡 Starting heartbeat monitoring for newly online peer: " + peerId);
                        
                        // Start UDP heartbeat monitoring (no TCP connection needed for status updates)
                        heartbeatService.startMonitoring(
                            peerId,
                            peerInfo.getIpAddress(),
                            peerInfo.getUdpPort()
                        );
                        
                        System.out.println("[P2PManager] ✅ Now monitoring peer " + peerId + " for status updates");
                    } else {
                        System.err.println("[P2PManager] ⚠️ Could not fetch P2P info for peer: " + peerId);
                    }
                } catch (Exception e) {
                    System.err.println("[P2PManager] ❌ Failed to start monitoring peer " + peerId + ": " + e.getMessage());
                }
            });
        }
        
        // Update peer session status if exists
        PeerSession session = connectionManager.getPeerSession(peerId);
        if (session != null) {
            session.updateHeartbeat();
        }
        
        // Forward to external listener (ChatScene/FriendScene)
        if (externalListener != null && externalListener instanceof StatusChangeListener) {
            ((StatusChangeListener) externalListener).onUserOnline(peerId);
        }
    }
    
    @Override
    public void onPeerOffline(Long peerId) {
        System.out.println("[P2PManager] 🔴 Peer went offline: " + peerId);
        
        // Forward to external listener (ChatScene/FriendScene)
        if (externalListener != null && externalListener instanceof StatusChangeListener) {
            ((StatusChangeListener) externalListener).onUserOffline(peerId);
        }
    }
    
    /**
     * Status Change Listener Interface
     * Implement this in UI components (ChatScene, FriendScene) to receive instant status updates
     */
    public interface StatusChangeListener {
        void onUserOnline(Long userId);
        void onUserOffline(Long userId);
    }
    
    // ===== Private Methods =====

    private void sendQueuedMessages(Long peerId) {
        if (!messageQueue.hasQueuedMessages(peerId)) {
            return;
        }

        List<P2PMessage> queuedMessages = messageQueue.dequeueAll(peerId);

        System.out.println("[P2PManager] 📤 Sending " + queuedMessages.size() + " queued messages to peer: " + peerId);

        for (P2PMessage message : queuedMessages) {
            connectionManager.sendMessage(peerId, message);
        }
    }
}
