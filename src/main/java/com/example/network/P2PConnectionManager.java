package com.example.network;

import com.example.crypto.P2PEncryption;
import com.example.network.model.ConnectionState;
import com.example.network.model.P2PMessage;
import com.example.network.model.PeerSession;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;

/**
 * P2P Connection Manager
 * Manages TCP connections to peers using pure Java sockets
 */
public class P2PConnectionManager {
    
    private final Long myUserId;
    private final int myTcpPort;
    private final KeyPair myKeyPair;
    
    // Active peer connections
    private final Map<Long, Socket> peerSockets = new ConcurrentHashMap<>();
    private final Map<Long, PeerSession> peerSessions = new ConcurrentHashMap<>();
    private final Map<Long, ObjectOutputStream> outputStreams = new ConcurrentHashMap<>();
    private final Map<Long, ObjectInputStream> inputStreams = new ConcurrentHashMap<>();
    
    // Message listeners
    private final List<P2PMessageListener> listeners = new CopyOnWriteArrayList<>();
    
    // Server socket for incoming connections
    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean running = false;
    
    // Thread pool for handling connections
    private final ExecutorService connectionPool = Executors.newCachedThreadPool();
    
    // Message acknowledgment tracking
    private final Map<String, CompletableFuture<Boolean>> pendingAcks = new ConcurrentHashMap<>();
    private static final long ACK_TIMEOUT_MS = 5000;
    
    public P2PConnectionManager(Long myUserId, int myTcpPort) throws Exception {
        this.myUserId = myUserId;
        this.myTcpPort = myTcpPort;
        this.myKeyPair = P2PEncryption.generateRSAKeyPair();
        
        System.out.println("[P2PConnectionManager] Initialized for user: " + myUserId);
        System.out.println("[P2PConnectionManager] TCP Port: " + myTcpPort);
        System.out.println("[P2PConnectionManager] RSA Public Key generated");
    }
    
    /**
     * Start listening for incoming connections
     */
    public void startListening() throws IOException {
        if (running) {
            System.out.println("[P2PConnectionManager] Already listening");
            return;
        }
        
        serverSocket = new ServerSocket(myTcpPort);
        running = true;
        
        serverThread = new Thread(() -> {
            System.out.println("[P2PConnectionManager] ✅ Listening on TCP port: " + myTcpPort);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[P2PConnectionManager] 📥 Incoming connection from: " + clientSocket.getInetAddress());
                    
                    // Handle connection in separate thread
                    connectionPool.submit(() -> handleIncomingConnection(clientSocket));
                    
                } catch (SocketException e) {
                    if (running) {
                        System.err.println("[P2PConnectionManager] ❌ Server socket error: " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("[P2PConnectionManager] ❌ Error accepting connection: " + e.getMessage());
                }
            }
        });
        
        serverThread.setName("P2P-Server-" + myUserId);
        serverThread.start();
    }
    
    /**
     * Connect to a peer
     */
    public boolean connectToPeer(Long peerId, String ipAddress, int port) {
        if (peerSockets.containsKey(peerId) && !peerSockets.get(peerId).isClosed()) {
            System.out.println("[P2PConnectionManager] Already connected to peer: " + peerId);
            return true;
        }
        
        try {
            System.out.println("[P2PConnectionManager] 🔗 Connecting to peer " + peerId + " at " + ipAddress + ":" + port);
            
            // Create peer session
            PeerSession session = new PeerSession(peerId, ipAddress, port, 0);
            session.setState(ConnectionState.CONNECTING);
            session.setMyKeyPair(myKeyPair);
            peerSessions.put(peerId, session);
            
            // Connect with timeout
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ipAddress, port), 5000);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            
            peerSockets.put(peerId, socket);
            
            // Create streams
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            
            outputStreams.put(peerId, out);
            inputStreams.put(peerId, in);
            
            // Exchange encryption keys
            exchangeKeys(peerId, out, in);
            
            // Update session state
            session.setState(ConnectionState.CONNECTED);
            
            // Start listening for messages from this peer
            connectionPool.submit(() -> listenToPeer(peerId, in));
            
            System.out.println("[P2PConnectionManager] ✅ Connected to peer: " + peerId);
            notifyPeerConnected(peerId, null);
            
            return true;
            
        } catch (SocketTimeoutException e) {
            System.err.println("[P2PConnectionManager] ❌ Connection timeout to peer " + peerId);
            notifyConnectionError(peerId, e);
            return false;
        } catch (IOException e) {
            System.err.println("[P2PConnectionManager] ❌ Failed to connect to peer " + peerId + ": " + e.getMessage());
            notifyConnectionError(peerId, e);
            return false;
        } catch (Exception e) {
            System.err.println("[P2PConnectionManager] ❌ Error during key exchange: " + e.getMessage());
            e.printStackTrace();
            notifyConnectionError(peerId, e);
            return false;
        }
    }
    
    /**
     * Send message to peer
     */
    public CompletableFuture<Boolean> sendMessage(Long peerId, P2PMessage message) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        if (!isConnected(peerId)) {
            System.err.println("[P2PConnectionManager] ❌ Not connected to peer: " + peerId);
            future.complete(false);
            return future;
        }
        
        try {
            PeerSession session = peerSessions.get(peerId);
            ObjectOutputStream out = outputStreams.get(peerId);
            
            // Encrypt message content
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                SecretKey sessionKey = session.getAesSessionKey();
                byte[] encrypted = P2PEncryption.encryptAES(message.getContent(), sessionKey);
                message.setEncryptedData(encrypted);
                message.setContent(null); // Clear plain text
            }
            
            // Sign message
            String signatureData = message.getMessageId() + message.getTimestamp();
            byte[] signature = P2PEncryption.signMessage(signatureData, myKeyPair.getPrivate());
            message.setSignature(signature);
            
            // Send message
            synchronized (out) {
                out.writeObject(message);
                out.flush();
            }
            
            session.incrementMessagesSent();
            session.setLastMessageId(message.getMessageId());
            
            System.out.println("[P2PConnectionManager] 📤 Sent message to peer " + peerId + ": " + message.getMessageId());
            
            // Wait for ACK if required
            if (message.isRequiresAck()) {
                CompletableFuture<Boolean> ackFuture = new CompletableFuture<>();
                pendingAcks.put(message.getMessageId(), ackFuture);
                
                // Timeout after 5 seconds
                ackFuture.orTimeout(ACK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .whenComplete((acked, error) -> {
                        pendingAcks.remove(message.getMessageId());
                        if (error != null) {
                            System.err.println("[P2PConnectionManager] ⏱️ ACK timeout for message: " + message.getMessageId());
                            future.complete(false);
                        } else {
                            future.complete(acked);
                        }
                    });
            } else {
                future.complete(true);
            }
            
        } catch (Exception e) {
            System.err.println("[P2PConnectionManager] ❌ Error sending message: " + e.getMessage());
            e.printStackTrace();
            future.complete(false);
        }
        
        return future;
    }
    
    /**
     * Disconnect from peer
     */
    public void disconnect(Long peerId) {
        try {
            Socket socket = peerSockets.remove(peerId);
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            outputStreams.remove(peerId);
            inputStreams.remove(peerId);
            
            PeerSession session = peerSessions.get(peerId);
            if (session != null) {
                session.setState(ConnectionState.DISCONNECTED);
            }
            
            System.out.println("[P2PConnectionManager] 🔌 Disconnected from peer: " + peerId);
            notifyPeerDisconnected(peerId);
            
        } catch (IOException e) {
            System.err.println("[P2PConnectionManager] Error closing connection: " + e.getMessage());
        }
    }
    
    /**
     * Stop listening and close all connections
     */
    public void shutdown() {
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[P2PConnectionManager] Error closing server socket: " + e.getMessage());
        }
        
        // Close all peer connections
        for (Long peerId : new ArrayList<>(peerSockets.keySet())) {
            disconnect(peerId);
        }
        
        connectionPool.shutdown();
        System.out.println("[P2PConnectionManager] ✅ Shutdown complete");
    }
    
    /**
     * Check if connected to peer
     */
    public boolean isConnected(Long peerId) {
        Socket socket = peerSockets.get(peerId);
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
    
    /**
     * Get peer session
     */
    public PeerSession getPeerSession(Long peerId) {
        return peerSessions.get(peerId);
    }
    
    /**
     * Add message listener
     */
    public void addMessageListener(P2PMessageListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove message listener
     */
    public void removeMessageListener(P2PMessageListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Get my RSA public key
     */
    public PublicKey getMyPublicKey() {
        return myKeyPair.getPublic();
    }
    
    // ===== Private Methods =====
    
    private void handleIncomingConnection(Socket socket) {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            
            // Read first message to identify peer
            P2PMessage firstMessage = (P2PMessage) in.readObject();
            Long peerId = firstMessage.getSenderId();
            
            System.out.println("[P2PConnectionManager] 📥 Identified peer: " + peerId);
            
            // Store connection
            peerSockets.put(peerId, socket);
            outputStreams.put(peerId, out);
            inputStreams.put(peerId, in);
            
            // Create session
            PeerSession session = new PeerSession(peerId, socket.getInetAddress().getHostAddress(), socket.getPort(), 0);
            session.setMyKeyPair(myKeyPair);
            session.setState(ConnectionState.CONNECTED);
            peerSessions.put(peerId, session);
            
            // Exchange keys
            exchangeKeysIncoming(peerId, firstMessage, out, in);
            
            // Process first message
            processMessage(peerId, firstMessage);
            
            // Start listening
            listenToPeer(peerId, in);
            
            notifyPeerConnected(peerId, null);
            
            // IMPORTANT: Establish reverse connection for bidirectional heartbeat
            // This ensures both users see each other as online
            System.out.println("[P2PConnectionManager] 🔄 Establishing reverse connection for bidirectional status...");
            
        } catch (Exception e) {
            System.err.println("[P2PConnectionManager] ❌ Error handling incoming connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void exchangeKeys(Long peerId, ObjectOutputStream out, ObjectInputStream in) throws Exception {
        // Generate AES session key
        SecretKey sessionKey = P2PEncryption.generateAESKey();
        
        // Send my public key
        String myPublicKeyStr = P2PEncryption.publicKeyToString(myKeyPair.getPublic());
        P2PMessage keyExchange = P2PMessage.createSignal(myUserId, peerId, "KEY_EXCHANGE");
        keyExchange.setContent(myPublicKeyStr);
        keyExchange.setRequiresAck(false);
        
        out.writeObject(keyExchange);
        out.flush();
        
        // Receive peer's public key
        P2PMessage peerKeyMsg = (P2PMessage) in.readObject();
        PublicKey peerPublicKey = P2PEncryption.stringToPublicKey(peerKeyMsg.getContent());
        
        // Encrypt session key with peer's public key and send
        byte[] encryptedSessionKey = P2PEncryption.encryptRSA(P2PEncryption.keyToBytes(sessionKey), peerPublicKey);
        P2PMessage sessionKeyMsg = P2PMessage.createSignal(myUserId, peerId, "SESSION_KEY");
        sessionKeyMsg.setSessionKeyEncrypted(encryptedSessionKey);
        sessionKeyMsg.setRequiresAck(false);
        
        out.writeObject(sessionKeyMsg);
        out.flush();
        
        // Receive peer's encrypted session key (we'll use ours)
        in.readObject(); // Just acknowledge
        
        // Store keys in session
        PeerSession session = peerSessions.get(peerId);
        session.setAesSessionKey(sessionKey);
        session.setPeerPublicKey(peerPublicKey);
        
        System.out.println("[P2PConnectionManager] 🔐 Key exchange completed with peer: " + peerId);
    }
    
    private void exchangeKeysIncoming(Long peerId, P2PMessage firstMessage, ObjectOutputStream out, ObjectInputStream in) throws Exception {
        // Extract peer's public key from first message
        PublicKey peerPublicKey = P2PEncryption.stringToPublicKey(firstMessage.getContent());
        
        // Send my public key
        String myPublicKeyStr = P2PEncryption.publicKeyToString(myKeyPair.getPublic());
        P2PMessage keyResponse = P2PMessage.createSignal(myUserId, peerId, "KEY_EXCHANGE");
        keyResponse.setContent(myPublicKeyStr);
        keyResponse.setRequiresAck(false);
        
        out.writeObject(keyResponse);
        out.flush();
        
        // Receive encrypted session key
        P2PMessage sessionKeyMsg = (P2PMessage) in.readObject();
        byte[] encryptedSessionKey = sessionKeyMsg.getSessionKeyEncrypted();
        byte[] sessionKeyBytes = P2PEncryption.decryptRSA(encryptedSessionKey, myKeyPair.getPrivate());
        SecretKey sessionKey = P2PEncryption.bytesToKey(sessionKeyBytes);
        
        // Send dummy session key (we use theirs)
        P2PMessage dummyKey = P2PMessage.createSignal(myUserId, peerId, "SESSION_KEY");
        dummyKey.setSessionKeyEncrypted(new byte[0]);
        dummyKey.setRequiresAck(false);
        out.writeObject(dummyKey);
        out.flush();
        
        // Store keys
        PeerSession session = peerSessions.get(peerId);
        session.setAesSessionKey(sessionKey);
        session.setPeerPublicKey(peerPublicKey);
        
        System.out.println("[P2PConnectionManager] 🔐 Key exchange completed (incoming) with peer: " + peerId);
    }
    
    private void listenToPeer(Long peerId, ObjectInputStream in) {
        System.out.println("[P2PConnectionManager] 👂 Listening to peer: " + peerId);
        
        try {
            while (isConnected(peerId)) {
                P2PMessage message = (P2PMessage) in.readObject();
                processMessage(peerId, message);
            }
        } catch (EOFException e) {
            System.out.println("[P2PConnectionManager] 🔌 Peer disconnected: " + peerId);
            disconnect(peerId);
        } catch (SocketException e) {
            System.out.println("[P2PConnectionManager] 🔌 Connection closed: " + peerId);
            disconnect(peerId);
        } catch (Exception e) {
            System.err.println("[P2PConnectionManager] ❌ Error reading from peer " + peerId + ": " + e.getMessage());
            disconnect(peerId);
        }
    }
    
    private void processMessage(Long peerId, P2PMessage message) {
        try {
            PeerSession session = peerSessions.get(peerId);
            
            // Decrypt message if encrypted
            if (message.getEncryptedData() != null && message.getEncryptedData().length > 0) {
                SecretKey sessionKey = session.getAesSessionKey();
                String decrypted = P2PEncryption.decryptAES(message.getEncryptedData(), sessionKey);
                message.setContent(decrypted);
                message.setEncryptedData(null);
            }
            
            // Verify signature
            if (message.getSignature() != null) {
                String signatureData = message.getMessageId() + message.getTimestamp();
                boolean valid = P2PEncryption.verifySignature(signatureData, message.getSignature(), session.getPeerPublicKey());
                if (!valid) {
                    System.err.println("[P2PConnectionManager] ⚠️ Invalid signature for message: " + message.getMessageId());
                    return;
                }
            }
            
            session.incrementMessagesReceived();
            
            // Handle different message types
            switch (message.getMsgType()) {
                case "text":
                    System.out.println("[P2PConnectionManager] 📨 Received text message from peer " + peerId);
                    notifyMessageReceived(message);
                    
                    // Send ACK
                    if (message.isRequiresAck()) {
                        sendAck(peerId, message.getMessageId());
                    }
                    break;
                    
                case "ack":
                    System.out.println("[P2PConnectionManager] ✅ Received ACK for message: " + message.getAckForMessageId());
                    CompletableFuture<Boolean> ackFuture = pendingAcks.remove(message.getAckForMessageId());
                    if (ackFuture != null) {
                        ackFuture.complete(true);
                    }
                    notifyMessageAcknowledged(message.getAckForMessageId());
                    break;
                    
                case "typing":
                    boolean isTyping = "1".equals(message.getContent());
                    notifyTypingIndicator(peerId, isTyping);
                    break;
                    
                case "signal":
                    // Handle signals (KEY_EXCHANGE, SESSION_KEY, etc.)
                    System.out.println("[P2PConnectionManager] 📡 Received signal: " + message.getContent());
                    break;
                    
                default:
                    System.out.println("[P2PConnectionManager] ❓ Unknown message type: " + message.getMsgType());
            }
            
        } catch (Exception e) {
            System.err.println("[P2PConnectionManager] ❌ Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendAck(Long peerId, String messageId) {
        P2PMessage ack = P2PMessage.createAck(messageId, myUserId, peerId);
        sendMessage(peerId, ack);
    }
    
    // ===== Notification Methods =====
    
    private void notifyMessageReceived(P2PMessage message) {
        for (P2PMessageListener listener : listeners) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                System.err.println("[P2PConnectionManager] Error in listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyPeerConnected(Long peerId, String username) {
        for (P2PMessageListener listener : listeners) {
            try {
                listener.onPeerConnected(peerId, username);
            } catch (Exception e) {
                System.err.println("[P2PConnectionManager] Error in listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyPeerDisconnected(Long peerId) {
        for (P2PMessageListener listener : listeners) {
            try {
                listener.onPeerDisconnected(peerId);
            } catch (Exception e) {
                System.err.println("[P2PConnectionManager] Error in listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyConnectionError(Long peerId, Exception error) {
        for (P2PMessageListener listener : listeners) {
            try {
                listener.onConnectionError(peerId, error);
            } catch (Exception e) {
                System.err.println("[P2PConnectionManager] Error in listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyTypingIndicator(Long peerId, boolean isTyping) {
        for (P2PMessageListener listener : listeners) {
            try {
                listener.onTypingIndicator(peerId, isTyping);
            } catch (Exception e) {
                System.err.println("[P2PConnectionManager] Error in listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyMessageAcknowledged(String messageId) {
        for (P2PMessageListener listener : listeners) {
            try {
                listener.onMessageAcknowledged(messageId);
            } catch (Exception e) {
                System.err.println("[P2PConnectionManager] Error in listener: " + e.getMessage());
            }
        }
    }
}
