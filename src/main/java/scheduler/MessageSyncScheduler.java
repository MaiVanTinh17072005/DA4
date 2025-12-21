package scheduler;

import dto.MessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
    
    /**
     * Sync pending messages from Redis to PostgreSQL
     * Runs every 5 seconds (5000ms)
     */
    @Scheduled(fixedDelay = 5000)
    public void syncPendingMessages() {
        try {
            List<MessageDTO> pendingMessages = redisQueue.getAllPendingMessages();
            
            if (pendingMessages.isEmpty()) {
                return; // No messages to sync
            }
            
            System.out.println("[MessageSync] 🔄 Starting sync of " + pendingMessages.size() + " pending messages...");
            
            int successful = 0;
            int failed = 0;
            
            for (MessageDTO message : pendingMessages) {
                try {
                    // Save to PostgreSQL
                    MessageDTO savedMessage = messageService.sendMessage(message);
                    
                    if (savedMessage != null) {
                        // Remove from Redis after successful save
                        redisQueue.removeMessage(message.getSenderId(), 
                            String.valueOf(message.getMsgId()));
                        successful++;
                        System.out.println("[MessageSync] ✅ Synced message " + message.getMsgId());
                    } else {
                        failed++;
                        System.err.println("[MessageSync] ❌ Failed to sync message " + message.getMsgId());
                    }
                    
                } catch (Exception e) {
                    failed++;
                    System.err.println("[MessageSync] ❌ Error syncing message " + message.getMsgId() + ": " + e.getMessage());
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
