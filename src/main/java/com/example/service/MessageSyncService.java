package com.example.service;

import com.example.api.dto.MessageDTO;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.*;

/**
 * Syncs pending messages to server when it comes online
 */
public class MessageSyncService implements ServerHealthMonitor.ServerStatusListener {
    
    private final ServerHealthMonitor healthMonitor;
    private final LocalRedisService localRedis;
    private final MessageService messageService;
    
    private final ScheduledExecutorService syncScheduler;
    private volatile boolean isSyncing = false;
    
    // Callback for UI updates
    public interface SyncStatusListener {
        void onSyncStarted(int messageCount);
        void onSyncProgress(int sent, int total);
        void onSyncCompleted(int successful, int failed);
    }
    
    private SyncStatusListener syncStatusListener;
    
    public MessageSyncService(ServerHealthMonitor healthMonitor, 
                             LocalRedisService localRedis,
                             MessageService messageService) {
        this.healthMonitor = healthMonitor;
        this.localRedis = localRedis;
        this.messageService = messageService;
        
        this.syncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MessageSyncService");
            t.setDaemon(true);
            return t;
        });
        
        // Register as listener
        healthMonitor.addListener(this);
        
        System.out.println("[MessageSyncService] ✅ Initialized");
    }
    
    /**
     * Set sync status listener for UI updates
     */
    public void setSyncStatusListener(SyncStatusListener listener) {
        this.syncStatusListener = listener;
    }
    
    /**
     * Start the sync service
     */
    public void start() {
        System.out.println("[MessageSyncService] 🚀 Started");
        
        // If server is already online, sync immediately
        if (healthMonitor.isServerOnline()) {
            scheduleSync(1000); // Sync after 1 second
        }
    }
    
    /**
     * Stop the sync service
     */
    public void stop() {
        healthMonitor.removeListener(this);
        syncScheduler.shutdown();
        System.out.println("[MessageSyncService] ⏹️ Stopped");
    }
    
    @Override
    public void onServerOnline() {
        System.out.println("[MessageSyncService] 🌐 Server came online, scheduling sync...");
        scheduleSync(2000); // Wait 2 seconds before syncing
    }
    
    @Override
    public void onServerOffline() {
        System.out.println("[MessageSyncService] 🔴 Server went offline");
        // Cancel any ongoing sync
        isSyncing = false;
    }
    
    /**
     * Schedule a sync operation
     */
    private void scheduleSync(long delayMs) {
        syncScheduler.schedule(this::syncPendingMessages, delayMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sync all pending messages to server
     */
    public void syncPendingMessages() {
        if (isSyncing) {
            System.out.println("[MessageSyncService] ⚠️ Sync already in progress");
            return;
        }
        
        if (!healthMonitor.isServerOnline()) {
            System.out.println("[MessageSyncService] ⚠️ Server offline, cannot sync");
            return;
        }
        
        // Get current user ID
        Long userId = com.example.util.SessionManager.getCurrentUserId();
        if (userId == null) {
            System.err.println("[MessageSyncService] ❌ No user logged in");
            return;
        }
        
        // Get pending messages from local Redis
        List<MessageDTO> pendingMessages = localRedis.getAllPendingMessages(userId);
        
        if (pendingMessages.isEmpty()) {
            System.out.println("[MessageSyncService] ✅ No pending messages to sync");
            return;
        }
        
        isSyncing = true;
        int totalMessages = pendingMessages.size();
        
        System.out.println("[MessageSyncService] 🔄 Starting sync of " + totalMessages + " messages from local Redis...");
        
        // Notify UI
        notifySyncStarted(totalMessages);
        
        // Sync messages one by one
        CompletableFuture.runAsync(() -> {
            int successful = 0;
            int failed = 0;
            
            for (int i = 0; i < pendingMessages.size(); i++) {
                if (!healthMonitor.isServerOnline()) {
                    System.out.println("[MessageSyncService] ⚠️ Server went offline during sync");
                    break;
                }
                
                MessageDTO message = pendingMessages.get(i);
                
                // ✅ ENCRYPTION STATUS (Allow both encrypted and plain text)
                boolean isEncrypted = message.getAesEncrypted() != null && message.getAesEncrypted();
                
                if (!isEncrypted) {
                    System.out.println("[MessageSyncService] ⚠️ Syncing PLAIN TEXT message: " + message.getMsgId());
                    System.out.println("  - Encrypted: false");
                    System.out.println("  - Content (plain): " + message.getContent().substring(0, Math.min(30, message.getContent().length())) + "...");
                } else {
                    System.out.println("[MessageSyncService] 🔐 Syncing ENCRYPTED message: " + message.getMsgId());
                    System.out.println("  - Encrypted: true");
                    System.out.println("  - Content (encrypted): " + message.getContent().substring(0, Math.min(30, message.getContent().length())) + "...");
                }
                
                // Ensure senderId is set
                if (message.getSenderId() == null) {
                    message.setSenderId(userId);
                    System.out.println("[MessageSyncService] ⚠️ Fixed null senderId: " + userId);
                }
                
                try {
                    
                    // Send to Redis queue via backend API
                    boolean queuedToRedis = queueMessageToRedis(message);
                    
                    if (queuedToRedis) {
                        // Success - remove from local Redis
                        localRedis.removeMessage(userId, String.valueOf(message.getMsgId()));
                        successful++;
                        System.out.println("[MessageSyncService] ✅ Synced to server Redis: " + (i + 1) + "/" + totalMessages);
                    } else {
                        // Failed - keep in local Redis for retry
                        failed++;
                        System.err.println("[MessageSyncService] ❌ Failed to sync: " + (i + 1) + "/" + totalMessages);
                    }
                    
                    // Notify progress
                    notifySyncProgress(i + 1, totalMessages);
                    
                    // Small delay between messages
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    failed++;
                    System.err.println("[MessageSyncService] ❌ Error syncing message: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            isSyncing = false;
            
            final int finalSuccessful = successful;
            final int finalFailed = failed;
            
            System.out.println("[MessageSyncService] ℹ️ Backend will sync from Redis to PostgreSQL every 5 seconds");
            
            // Notify completion
            notifySyncCompleted(finalSuccessful, finalFailed);
        });
    }
    
    /**
     * Queue a message to Redis via backend API
     */
    private boolean queueMessageToRedis(MessageDTO message) {
        try {
            String url = com.example.config.ApiConfig.BASE_URL + "/api/v1/messages/queue";
            
            System.out.println("[MessageSyncService] 📤 Queueing message to Redis: " + message.getMsgId());
            
            // Create HTTP connection
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // Convert message to JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(message);
            
            // Send request
            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Check response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                System.out.println("[MessageSyncService] ✅ Message queued to Redis successfully");
                return true;
            } else {
                System.err.println("[MessageSyncService] ❌ Failed to queue. Response code: " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("[MessageSyncService] ❌ Error queueing to Redis: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Manually trigger sync
     */
    public void triggerSync() {
        System.out.println("[MessageSyncService] 🔄 Manual sync triggered");
        syncPendingMessages();
    }
    
    /**
     * Notify UI that sync started
     */
    private void notifySyncStarted(int messageCount) {
        if (syncStatusListener != null) {
            Platform.runLater(() -> syncStatusListener.onSyncStarted(messageCount));
        }
    }
    
    /**
     * Notify UI of sync progress
     */
    private void notifySyncProgress(int sent, int total) {
        if (syncStatusListener != null) {
            Platform.runLater(() -> syncStatusListener.onSyncProgress(sent, total));
        }
    }
    
    /**
     * Notify UI that sync completed
     */
    private void notifySyncCompleted(int successful, int failed) {
        if (syncStatusListener != null) {
            Platform.runLater(() -> syncStatusListener.onSyncCompleted(successful, failed));
        }
    }
}
