package com.example.service;

import com.example.api.dto.MessageDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pending messages that need to be synced to server
 */
public class LocalMessageQueue {
    
    private static final String QUEUE_FILE_NAME = "pending_messages.json";
    private final Path queueFilePath;
    private final Gson gson;
    
    // Message status
    public enum MessageStatus {
        PENDING,    // Waiting to be sent
        SYNCING,    // Currently being sent
        SENT,       // Successfully sent
        FAILED      // Failed to send
    }
    
    // Wrapper for message with metadata
    public static class PendingMessage {
        private MessageDTO message;
        private MessageStatus status;
        private LocalDateTime createdAt;
        private int retryCount;
        
        public PendingMessage(MessageDTO message) {
            this.message = message;
            this.status = MessageStatus.PENDING;
            this.createdAt = LocalDateTime.now();
            this.retryCount = 0;
        }
        
        // Getters and setters
        public MessageDTO getMessage() { return message; }
        public MessageStatus getStatus() { return status; }
        public void setStatus(MessageStatus status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public int getRetryCount() { return retryCount; }
        public void incrementRetryCount() { this.retryCount++; }
    }
    
    private final Map<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();
    
    public LocalMessageQueue() {
        // Determine queue file path (in user home directory)
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, ".discord_mini");
        
        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            System.err.println("[LocalMessageQueue] Failed to create app directory: " + e.getMessage());
        }
        
        this.queueFilePath = appDir.resolve(QUEUE_FILE_NAME);
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();
        
        // Load existing messages
        loadFromDisk();
        
        System.out.println("[LocalMessageQueue] ✅ Initialized with " + pendingMessages.size() + " pending messages");
    }
    
    /**
     * Add a message to pending queue
     */
    public void addPendingMessage(MessageDTO message) {
        // Generate unique ID if not present
        if (message.getMsgId() == null) {
            // Use timestamp-based ID for uniqueness
            String uniqueId = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            message.setMsgId(Long.parseLong(String.valueOf(System.currentTimeMillis())));
        }
        
        PendingMessage pending = new PendingMessage(message);
        String messageKey = String.valueOf(message.getMsgId());
        pendingMessages.put(messageKey, pending);
        
        System.out.println("[LocalMessageQueue] ➕ Added pending message: " + messageKey);
        
        // Save to disk
        saveToDisk();
    }
    
    /**
     * Get all pending messages
     */
    public List<MessageDTO> getPendingMessages() {
        List<MessageDTO> messages = new ArrayList<>();
        for (PendingMessage pm : pendingMessages.values()) {
            if (pm.getStatus() == MessageStatus.PENDING) {
                messages.add(pm.getMessage());
            }
        }
        return messages;
    }
    
    /**
     * Get all pending message wrappers (with metadata)
     */
    public List<PendingMessage> getPendingMessageWrappers() {
        List<PendingMessage> messages = new ArrayList<>();
        for (PendingMessage pm : pendingMessages.values()) {
            if (pm.getStatus() == MessageStatus.PENDING) {
                messages.add(pm);
            }
        }
        return messages;
    }
    
    /**
     * Mark message as syncing
     */
    public void markAsSyncing(String messageId) {
        PendingMessage pm = pendingMessages.get(messageId);
        if (pm != null) {
            pm.setStatus(MessageStatus.SYNCING);
            System.out.println("[LocalMessageQueue] 🔄 Syncing message: " + messageId);
        }
    }
    
    /**
     * Mark message as sent and remove from queue
     */
    public void markAsSent(String messageId) {
        PendingMessage pm = pendingMessages.remove(messageId);
        if (pm != null) {
            System.out.println("[LocalMessageQueue] ✅ Message sent: " + messageId);
            saveToDisk();
        }
    }
    
    /**
     * Mark message as failed
     */
    public void markAsFailed(String messageId) {
        PendingMessage pm = pendingMessages.get(messageId);
        if (pm != null) {
            pm.setStatus(MessageStatus.FAILED);
            pm.incrementRetryCount();
            System.out.println("[LocalMessageQueue] ❌ Message failed (retry " + pm.getRetryCount() + "): " + messageId);
            saveToDisk();
        }
    }
    
    /**
     * Reset failed messages to pending for retry
     */
    public void retryFailedMessages() {
        int count = 0;
        for (PendingMessage pm : pendingMessages.values()) {
            if (pm.getStatus() == MessageStatus.FAILED) {
                pm.setStatus(MessageStatus.PENDING);
                count++;
            }
        }
        if (count > 0) {
            System.out.println("[LocalMessageQueue] 🔄 Reset " + count + " failed messages to pending");
            saveToDisk();
        }
    }
    
    /**
     * Get count of pending messages
     */
    public int getPendingCount() {
        return (int) pendingMessages.values().stream()
            .filter(pm -> pm.getStatus() == MessageStatus.PENDING)
            .count();
    }
    
    /**
     * Save queue to disk
     */
    public void saveToDisk() {
        try {
            String json = gson.toJson(new ArrayList<>(pendingMessages.values()));
            Files.write(queueFilePath, json.getBytes(StandardCharsets.UTF_8));
            System.out.println("[LocalMessageQueue] 💾 Saved " + pendingMessages.size() + " messages to disk");
        } catch (IOException e) {
            System.err.println("[LocalMessageQueue] ❌ Failed to save queue: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load queue from disk
     */
    private void loadFromDisk() {
        if (!Files.exists(queueFilePath)) {
            System.out.println("[LocalMessageQueue] No existing queue file found");
            return;
        }
        
        try {
            String json = new String(Files.readAllBytes(queueFilePath), StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<PendingMessage>>(){}.getType();
            List<PendingMessage> loaded = gson.fromJson(json, listType);
            
            if (loaded != null) {
                for (PendingMessage pm : loaded) {
                    String messageKey = String.valueOf(pm.getMessage().getMsgId());
                    pendingMessages.put(messageKey, pm);
                }
                System.out.println("[LocalMessageQueue] 📂 Loaded " + loaded.size() + " messages from disk");
            }
        } catch (IOException e) {
            System.err.println("[LocalMessageQueue] ❌ Failed to load queue: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear all messages
     */
    public void clear() {
        pendingMessages.clear();
        saveToDisk();
        System.out.println("[LocalMessageQueue] 🗑️ Cleared all messages");
    }
}
