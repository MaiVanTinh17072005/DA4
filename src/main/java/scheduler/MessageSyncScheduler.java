package scheduler;

import dto.MessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import repository.MessageRepository;
import service.MessageService;
import service.RedisMessageQueueService;

import java.util.List;

/**
 * Scheduled task to sync pending messages from Redis to PostgreSQL
 * Runs every 5 seconds and on application startup
 */
@Component
@EnableScheduling
public class MessageSyncScheduler {
    
    @Autowired
    private RedisMessageQueueService redisQueue;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private MessageRepository messageRepository;
    
    /**
     * Sync pending messages from Redis to PostgreSQL
     * Runs every 5 seconds (5000ms)
     * ✅ SYNCHRONIZED to prevent concurrent execution from @Scheduled and @EventListener
     */
    @Scheduled(fixedDelay = 5000)
    public synchronized void syncPendingMessages() {
        try {
            List<MessageDTO> pendingMessages = redisQueue.getAllPendingMessages();
            
            if (pendingMessages.isEmpty()) {
                return; // No messages to sync
            }
            
            System.out.println("[MessageSync] 🔄 Starting sync of " + pendingMessages.size() + " pending messages...");
            System.out.println("[MessageSync] 🔒 Thread: " + Thread.currentThread().getName());
            
            int successful = 0;
            int failed = 0;
            
            for (MessageDTO message : pendingMessages) {
                try {
                    System.out.println("[MessageSync] 🔄 Processing message " + message.getMsgId());
                    System.out.println("  - SenderId: " + message.getSenderId());
                    System.out.println("  - ReceiverId: " + message.getReceiverId());
                    System.out.println("  - Content: " + (message.getContent() != null ? message.getContent().substring(0, Math.min(20, message.getContent().length())) + "..." : "null"));
                    System.out.println("  - Encrypted: " + message.getAesEncrypted());
                    
                    // ✅ FIX: Check if message already exists in PostgreSQL
                    // This prevents duplicate storage when syncing from Redis
                    if (messageRepository.existsById(message.getMsgId())) {
                        System.out.println("[MessageSync] ⚠️ Message " + message.getMsgId() + " already exists in DB - skipping");
                        // Remove from Redis queue since it's already in DB
                        redisQueue.removeMessage(message.getSenderId(), 
                            String.valueOf(message.getMsgId()));
                        successful++;
                        continue;
                    }
                    
                    // Save to PostgreSQL
                    MessageDTO savedMessage = messageService.sendMessage(message);
                    
                    if (savedMessage != null) {
                        // Remove from Redis ONLY after successful save
                        redisQueue.removeMessage(message.getSenderId(), 
                            String.valueOf(message.getMsgId()));
                        successful++;
                        System.out.println("[MessageSync] ✅ Synced message " + message.getMsgId());
                    } else {
                        // DO NOT remove from Redis if save failed
                        failed++;
                        System.err.println("[MessageSync] ❌ Failed to sync message " + message.getMsgId() + " - messageService returned null");
                        System.err.println("  Message will remain in Redis for retry");
                    }
                    
                } catch (Exception e) {
                    // DO NOT remove from Redis if exception occurred
                    failed++;
                    System.err.println("[MessageSync] ❌ Error syncing message " + message.getMsgId() + ": " + e.getMessage());
                    System.err.println("  Full stack trace:");
                    e.printStackTrace();  // CRITICAL: Print full stack trace to see actual error
                    System.err.println("  Message will remain in Redis for retry");
                }
            }
            
            System.out.println("[MessageSync] ✅ Sync completed: " + successful + " successful, " + failed + " failed");
            
        } catch (Exception e) {
            System.err.println("[MessageSync] ❌ Error in sync task: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sync pending messages on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        System.out.println("[MessageSync] 🚀 Application started - syncing pending messages from Redis...");
        syncPendingMessages();
    }
}
