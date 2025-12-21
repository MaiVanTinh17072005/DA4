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
    private final LocalMessageQueue messageQueue;
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
                             LocalMessageQueue messageQueue,
                             MessageService messageService) {
        this.healthMonitor = healthMonitor;
        this.messageQueue = messageQueue;
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
        
        List<LocalMessageQueue.PendingMessage> pendingMessages = messageQueue.getPendingMessageWrappers();
        
        if (pendingMessages.isEmpty()) {
            System.out.println("[MessageSyncService] ✅ No pending messages to sync");
            return;
        }
        
        isSyncing = true;
        int totalMessages = pendingMessages.size();
        
        System.out.println("[MessageSyncService] 🔄 Starting sync of " + totalMessages + " messages...");
        
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
                
                LocalMessageQueue.PendingMessage pm = pendingMessages.get(i);
                MessageDTO message = pm.getMessage();
                
                try {
                    // Mark as syncing (use msgId as String key)
                    String messageKey = String.valueOf(message.getMsgId());
                    messageQueue.markAsSyncing(messageKey);
                    
                    // Send to server
                    MessageDTO result = messageService.sendMessage(message);
                    
                    if (result != null) {
                        // Success
                        messageQueue.markAsSent(messageKey);
                        successful++;
                        System.out.println("[MessageSyncService] ✅ Synced message " + (i + 1) + "/" + totalMessages);
                    } else {
                        // Failed
                        messageQueue.markAsFailed(messageKey);
                        failed++;
                        System.err.println("[MessageSyncService] ❌ Failed to sync message " + (i + 1) + "/" + totalMessages);
                    }
                    
                    // Notify progress
                    final int currentIndex = i + 1;
                    notifySyncProgress(currentIndex, totalMessages);
                    
                    // Small delay between messages to avoid overwhelming server
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    String messageKey = String.valueOf(message.getMsgId());
                    messageQueue.markAsFailed(messageKey);
                    failed++;
                    System.err.println("[MessageSyncService] ❌ Error syncing message: " + e.getMessage());
                }
            }
            
            isSyncing = false;
            
            final int finalSuccessful = successful;
            final int finalFailed = failed;
            
            System.out.println("[MessageSyncService] ✅ Sync completed: " + finalSuccessful + " successful, " + finalFailed + " failed");
            
            // Notify completion
            notifySyncCompleted(finalSuccessful, finalFailed);
        });
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
