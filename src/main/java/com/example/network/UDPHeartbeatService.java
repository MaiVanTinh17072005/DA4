package com.example.network;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;

/**
 * UDP Heartbeat Service
 * Sends periodic heartbeat packets to keep connections alive
 */
public class UDPHeartbeatService {
    
    private final Long myUserId;
    private final int myUdpPort;
    private DatagramSocket udpSocket;
    
    // Heartbeat tracking
    private final Map<Long, InetSocketAddress> peerAddresses = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastHeartbeatReceived = new ConcurrentHashMap<>();
    
    // Scheduler for sending heartbeats
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Long, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    
    // Listener thread
    private Thread listenerThread;
    private volatile boolean running = false;
    
    // Heartbeat configuration
    private static final int HEARTBEAT_INTERVAL_MS = 5000;  // 5 seconds
    private static final int HEARTBEAT_TIMEOUT_MS = 15000;  // 15 seconds
    private static final byte HEARTBEAT_PING = 0x01;
    private static final byte HEARTBEAT_PONG = 0x02;
    private static final byte STATUS_ONLINE = 0x03;
    private static final byte STATUS_OFFLINE = 0x04;
    
    // Callbacks
    private HeartbeatListener listener;
    
    public UDPHeartbeatService(Long myUserId, int myUdpPort) {
        this.myUserId = myUserId;
        this.myUdpPort = myUdpPort;
    }
    
    /**
     * Start UDP heartbeat service
     */
    public void start() throws SocketException {
        if (running) {
            System.out.println("[UDPHeartbeat] Already running");
            return;
        }
        
        udpSocket = new DatagramSocket(myUdpPort);
        running = true;
        
        // Start listener thread
        listenerThread = new Thread(this::listenForHeartbeats);
        listenerThread.setName("UDP-Heartbeat-Listener-" + myUserId);
        listenerThread.start();
        
        System.out.println("[UDPHeartbeat] ✅ Started on UDP port: " + myUdpPort);
    }
    
    /**
     * Start sending heartbeats to a peer
     */
    public void startHeartbeat(Long peerId, String ipAddress, int udpPort) {
        InetSocketAddress address = new InetSocketAddress(ipAddress, udpPort);
        peerAddresses.put(peerId, address);
        lastHeartbeatReceived.put(peerId, System.currentTimeMillis());
        
        // Schedule periodic heartbeat
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
            () -> sendHeartbeat(peerId),
            0,
            HEARTBEAT_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        
        heartbeatTasks.put(peerId, task);
        
        System.out.println("[UDPHeartbeat] 💓 Started heartbeat to peer: " + peerId);
    }
    
    /**
     * Stop heartbeat to a peer
     */
    public void stopHeartbeat(Long peerId) {
        ScheduledFuture<?> task = heartbeatTasks.remove(peerId);
        if (task != null) {
            task.cancel(false);
        }
        
        peerAddresses.remove(peerId);
        lastHeartbeatReceived.remove(peerId);
        
        System.out.println("[UDPHeartbeat] 🛑 Stopped heartbeat to peer: " + peerId);
    }
    
    /**
     * Check if peer is alive
     */
    public boolean isPeerAlive(Long peerId) {
        Long lastHeartbeat = lastHeartbeatReceived.get(peerId);
        if (lastHeartbeat == null) {
            return false;
        }
        
        long timeSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeat;
        return timeSinceLastHeartbeat < HEARTBEAT_TIMEOUT_MS;
    }
    
    /**
     * Get time since last heartbeat
     */
    public long getTimeSinceLastHeartbeat(Long peerId) {
        Long lastHeartbeat = lastHeartbeatReceived.get(peerId);
        if (lastHeartbeat == null) {
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - lastHeartbeat;
    }
    
    /**
     * Check if currently monitoring a peer
     */
    public boolean isMonitoring(Long peerId) {
        return peerAddresses.containsKey(peerId);
    }
    
    /**
     * Start monitoring a peer (alias for startHeartbeat for clarity)
     */
    public void startMonitoring(Long peerId, String ipAddress, int udpPort) {
        startHeartbeat(peerId, ipAddress, udpPort);
    }
    
    /**
     * Set heartbeat listener
     */
    public void setListener(HeartbeatListener listener) {
        this.listener = listener;
    }
    
    /**
     * Send ONLINE signal to a specific peer (instant status update)
     */
    public void sendOnlineSignal(Long peerId) {
        try {
            InetSocketAddress address = peerAddresses.get(peerId);
            if (address == null) {
                System.err.println("[UDPHeartbeat] ⚠️ Cannot send ONLINE signal - peer address not found: " + peerId);
                return;
            }
            
            // Create ONLINE packet: [STATUS_ONLINE][userId]
            ByteBuffer buffer = ByteBuffer.allocate(9);
            buffer.put(STATUS_ONLINE);
            buffer.putLong(myUserId);
            
            byte[] data = buffer.array();
            DatagramPacket packet = new DatagramPacket(data, data.length, address);
            
            udpSocket.send(packet);
            
            System.out.println("[UDPHeartbeat] 🟢 Sent ONLINE signal to peer: " + peerId);
            
        } catch (IOException e) {
            System.err.println("[UDPHeartbeat] ❌ Error sending ONLINE signal to peer " + peerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Send OFFLINE signal to a specific peer (instant status update)
     */
    public void sendOfflineSignal(Long peerId) {
        try {
            InetSocketAddress address = peerAddresses.get(peerId);
            if (address == null) {
                System.err.println("[UDPHeartbeat] ⚠️ Cannot send OFFLINE signal - peer address not found: " + peerId);
                return;
            }
            
            // Create OFFLINE packet: [STATUS_OFFLINE][userId]
            ByteBuffer buffer = ByteBuffer.allocate(9);
            buffer.put(STATUS_OFFLINE);
            buffer.putLong(myUserId);
            
            byte[] data = buffer.array();
            DatagramPacket packet = new DatagramPacket(data, data.length, address);
            
            udpSocket.send(packet);
            
            System.out.println("[UDPHeartbeat] 🔴 Sent OFFLINE signal to peer: " + peerId);
            
        } catch (IOException e) {
            System.err.println("[UDPHeartbeat] ❌ Error sending OFFLINE signal to peer " + peerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Broadcast ONLINE signal to all monitored peers
     * Call this when user logs in
     */
    public void broadcastOnlineToAll() {
        System.out.println("[UDPHeartbeat] 📢 Broadcasting ONLINE to all monitored peers (" + peerAddresses.size() + " peers)");
        
        for (Long peerId : peerAddresses.keySet()) {
            sendOnlineSignal(peerId);
        }
    }
    
    /**
     * Broadcast ONLINE signal to specific peer by ID
     * Fetches P2P info if not already monitoring
     */
    public void broadcastOnlineToPeer(Long peerId, String ipAddress, int udpPort) {
        try {
            // Create ONLINE packet: [STATUS_ONLINE][userId]
            ByteBuffer buffer = ByteBuffer.allocate(9);
            buffer.put(STATUS_ONLINE);
            buffer.putLong(myUserId);
            
            byte[] data = buffer.array();
            InetSocketAddress address = new InetSocketAddress(ipAddress, udpPort);
            DatagramPacket packet = new DatagramPacket(data, data.length, address);
            
            udpSocket.send(packet);
            
            System.out.println("[UDPHeartbeat] 🟢 Sent ONLINE signal to peer: " + peerId + " at " + ipAddress + ":" + udpPort);
            
        } catch (IOException e) {
            System.err.println("[UDPHeartbeat] ❌ Error broadcasting ONLINE to peer " + peerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Broadcast OFFLINE signal to all monitored peers
     * Call this when user logs out
     */
    public void broadcastOfflineToAll() {
        System.out.println("[UDPHeartbeat] 📢 Broadcasting OFFLINE to all monitored peers (" + peerAddresses.size() + " peers)");
        
        for (Long peerId : peerAddresses.keySet()) {
            sendOfflineSignal(peerId);
        }
    }
    
    /**
     * Broadcast OFFLINE signal to specific peer by ID
     * Sends directly to specified IP and port
     */
    public void broadcastOfflineToPeer(Long peerId, String ipAddress, int udpPort) {
        try {
            // Create OFFLINE packet: [STATUS_OFFLINE][userId]
            ByteBuffer buffer = ByteBuffer.allocate(9);
            buffer.put(STATUS_OFFLINE);
            buffer.putLong(myUserId);
            
            byte[] data = buffer.array();
            InetSocketAddress address = new InetSocketAddress(ipAddress, udpPort);
            DatagramPacket packet = new DatagramPacket(data, data.length, address);
            
            udpSocket.send(packet);
            
            System.out.println("[UDPHeartbeat] 🔴 Sent OFFLINE signal to peer: " + peerId + " at " + ipAddress + ":" + udpPort);
            
        } catch (IOException e) {
            System.err.println("[UDPHeartbeat] ❌ Error broadcasting OFFLINE to peer " + peerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Shutdown heartbeat service
     */
    public void shutdown() {
        running = false;
        
        // Cancel all heartbeat tasks
        for (ScheduledFuture<?> task : heartbeatTasks.values()) {
            task.cancel(false);
        }
        heartbeatTasks.clear();
        
        scheduler.shutdown();
        
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        
        System.out.println("[UDPHeartbeat] ✅ Shutdown complete");
    }
    
    // ===== Private Methods =====
    
    private void sendHeartbeat(Long peerId) {
        try {
            InetSocketAddress address = peerAddresses.get(peerId);
            if (address == null) {
                return;
            }
            
            // Create heartbeat packet: [PING][userId]
            ByteBuffer buffer = ByteBuffer.allocate(9);
            buffer.put(HEARTBEAT_PING);
            buffer.putLong(myUserId);
            
            byte[] data = buffer.array();
            DatagramPacket packet = new DatagramPacket(data, data.length, address);
            
            udpSocket.send(packet);
            
            // Check if peer is still alive
            if (!isPeerAlive(peerId)) {
                System.out.println("[UDPHeartbeat] ⚠️ Peer " + peerId + " not responding");
                if (listener != null) {
                    listener.onPeerTimeout(peerId);
                }
            }
            
        } catch (IOException e) {
            System.err.println("[UDPHeartbeat] ❌ Error sending heartbeat to peer " + peerId + ": " + e.getMessage());
        }
    }
    
    private void listenForHeartbeats() {
        System.out.println("[UDPHeartbeat] 👂 Listening for heartbeats...");
        
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        while (running) {
            try {
                udpSocket.receive(packet);
                
                ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                byte type = bb.get();
                long senderId = bb.getLong();
                
                if (type == HEARTBEAT_PING) {
                    // Received PING, send PONG
                    sendPong(packet.getAddress(), packet.getPort(), senderId);
                    
                    // Update last heartbeat
                    lastHeartbeatReceived.put(senderId, System.currentTimeMillis());
                    
                } else if (type == HEARTBEAT_PONG) {
                    // Received PONG
                    lastHeartbeatReceived.put(senderId, System.currentTimeMillis());
                    
                    if (listener != null) {
                        listener.onHeartbeatReceived(senderId);
                    }
                    
                } else if (type == STATUS_ONLINE) {
                    // Received ONLINE signal - instant status update
                    System.out.println("[UDPHeartbeat] 🟢 Received ONLINE signal from peer: " + senderId);
                    
                    // Update last heartbeat timestamp
                    lastHeartbeatReceived.put(senderId, System.currentTimeMillis());
                    
                    // Notify listener
                    if (listener != null) {
                        listener.onPeerOnline(senderId);
                    }
                    
                } else if (type == STATUS_OFFLINE) {
                    // Received OFFLINE signal - instant status update
                    System.out.println("[UDPHeartbeat] 🔴 Received OFFLINE signal from peer: " + senderId);
                    
                    // Notify listener
                    if (listener != null) {
                        listener.onPeerOffline(senderId);
                    }
                }
                
            } catch (SocketException e) {
                if (running) {
                    System.err.println("[UDPHeartbeat] ❌ Socket error: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("[UDPHeartbeat] ❌ Error receiving heartbeat: " + e.getMessage());
            }
        }
    }
    
    private void sendPong(InetAddress address, int port, long peerId) {
        try {
            // Create PONG packet: [PONG][userId]
            ByteBuffer buffer = ByteBuffer.allocate(9);
            buffer.put(HEARTBEAT_PONG);
            buffer.putLong(myUserId);
            
            byte[] data = buffer.array();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            
            udpSocket.send(packet);
            
        } catch (IOException e) {
            System.err.println("[UDPHeartbeat] ❌ Error sending PONG: " + e.getMessage());
        }
    }
    
    /**
     * Heartbeat Listener Interface
     */
    public interface HeartbeatListener {
        void onHeartbeatReceived(Long peerId);
        void onPeerTimeout(Long peerId);
        
        // Instant status update callbacks
        void onPeerOnline(Long peerId);
        void onPeerOffline(Long peerId);
    }
}
