package com.example.network;

import com.example.network.model.P2PMessage;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Message Queue System
 * Queues messages when peer is offline and sends them when peer comes back online
 */
public class MessageQueue {
    
    private final Long myUserId;
    
    // Queues for each peer
    private final Map<Long, Queue<P2PMessage>> peerQueues = new ConcurrentHashMap<>();
    
    // Queue persistence
    private final File queueDirectory;
    private static final int MAX_QUEUE_SIZE = 100;
    
    public MessageQueue(Long myUserId, String queueDir) {
        this.myUserId = myUserId;
        this.queueDirectory = new File(queueDir, "message_queue_" + myUserId);
        
        if (!queueDirectory.exists()) {
            queueDirectory.mkdirs();
        }
        
        System.out.println("[MessageQueue] Initialized for user: " + myUserId);
        System.out.println("[MessageQueue] Queue directory: " + queueDirectory.getAbsolutePath());
    }
    
    /**
     * Add message to queue
     */
    public boolean enqueue(Long peerId, P2PMessage message) {
        Queue<P2PMessage> queue = peerQueues.computeIfAbsent(peerId, k -> new ConcurrentLinkedQueue<>());
        
        if (queue.size() >= MAX_QUEUE_SIZE) {
            System.err.println("[MessageQueue] ⚠️ Queue full for peer " + peerId + ", dropping oldest message");
            queue.poll(); // Remove oldest
        }
        
        boolean added = queue.offer(message);
        
        if (added) {
            System.out.println("[MessageQueue] ➕ Queued message for peer " + peerId + " (queue size: " + queue.size() + ")");
            saveQueue(peerId);
        }
        
        return added;
    }
    
    /**
     * Get all queued messages for a peer
     */
    public List<P2PMessage> getQueuedMessages(Long peerId) {
        Queue<P2PMessage> queue = peerQueues.get(peerId);
        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(queue);
    }
    
    /**
     * Dequeue and return all messages for a peer
     */
    public List<P2PMessage> dequeueAll(Long peerId) {
        Queue<P2PMessage> queue = peerQueues.remove(peerId);
        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<P2PMessage> messages = new ArrayList<>(queue);
        System.out.println("[MessageQueue] 📤 Dequeued " + messages.size() + " messages for peer " + peerId);
        
        deleteQueue(peerId);
        
        return messages;
    }
    
    /**
     * Get queue size for a peer
     */
    public int getQueueSize(Long peerId) {
        Queue<P2PMessage> queue = peerQueues.get(peerId);
        return queue != null ? queue.size() : 0;
    }
    
    /**
     * Check if queue has messages for a peer
     */
    public boolean hasQueuedMessages(Long peerId) {
        return getQueueSize(peerId) > 0;
    }
    
    /**
     * Clear queue for a peer
     */
    public void clearQueue(Long peerId) {
        peerQueues.remove(peerId);
        deleteQueue(peerId);
        System.out.println("[MessageQueue] 🗑️ Cleared queue for peer: " + peerId);
    }
    
    /**
     * Clear all queues
     */
    public void clearAllQueues() {
        peerQueues.clear();
        
        File[] queueFiles = queueDirectory.listFiles();
        if (queueFiles != null) {
            for (File file : queueFiles) {
                file.delete();
            }
        }
        
        System.out.println("[MessageQueue] 🗑️ Cleared all queues");
    }
    
    /**
     * Save queue to disk
     */
    private void saveQueue(Long peerId) {
        try {
            File queueFile = new File(queueDirectory, "queue_" + peerId + ".dat");
            
            Queue<P2PMessage> queue = peerQueues.get(peerId);
            if (queue == null || queue.isEmpty()) {
                return;
            }
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(queueFile))) {
                oos.writeObject(new ArrayList<>(queue));
            }
            
        } catch (IOException e) {
            System.err.println("[MessageQueue] ❌ Error saving queue for peer " + peerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Load queue from disk
     */
    @SuppressWarnings("unchecked")
    public void loadQueue(Long peerId) {
        try {
            File queueFile = new File(queueDirectory, "queue_" + peerId + ".dat");
            
            if (!queueFile.exists()) {
                return;
            }
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(queueFile))) {
                List<P2PMessage> messages = (List<P2PMessage>) ois.readObject();
                
                Queue<P2PMessage> queue = new ConcurrentLinkedQueue<>(messages);
                peerQueues.put(peerId, queue);
                
                System.out.println("[MessageQueue] 📥 Loaded " + messages.size() + " queued messages for peer " + peerId);
            }
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[MessageQueue] ❌ Error loading queue for peer " + peerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Load all queues from disk
     */
    public void loadAllQueues() {
        File[] queueFiles = queueDirectory.listFiles((dir, name) -> name.startsWith("queue_") && name.endsWith(".dat"));
        
        if (queueFiles == null || queueFiles.length == 0) {
            System.out.println("[MessageQueue] No queued messages found");
            return;
        }
        
        for (File queueFile : queueFiles) {
            try {
                String filename = queueFile.getName();
                String peerIdStr = filename.substring(6, filename.length() - 4); // Extract peer ID
                Long peerId = Long.parseLong(peerIdStr);
                
                loadQueue(peerId);
                
            } catch (NumberFormatException e) {
                System.err.println("[MessageQueue] ❌ Invalid queue file: " + queueFile.getName());
            }
        }
        
        System.out.println("[MessageQueue] ✅ Loaded queues for " + peerQueues.size() + " peers");
    }
    
    /**
     * Delete queue file
     */
    private void deleteQueue(Long peerId) {
        File queueFile = new File(queueDirectory, "queue_" + peerId + ".dat");
        if (queueFile.exists()) {
            queueFile.delete();
        }
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPeers", peerQueues.size());
        
        int totalMessages = 0;
        for (Queue<P2PMessage> queue : peerQueues.values()) {
            totalMessages += queue.size();
        }
        stats.put("totalQueuedMessages", totalMessages);
        
        return stats;
    }
}
