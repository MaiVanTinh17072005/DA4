package com.example.util;

import com.example.api.dto.FriendNotificationDTO;
import com.example.service.FriendService;
import javafx.application.Platform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Notification Poller
 * Polls for friend request notifications and triggers callbacks
 */
public class NotificationPoller {
    
    private static NotificationPoller instance;
    private final FriendService friendService;
    private Timer pollingTimer;
    private Consumer<FriendNotificationDTO> onNotificationReceived;
    private Set<String> processedNotificationIds;
    private boolean isRunning = false;
    
    private static final int POLLING_INTERVAL = 5000; // 5 seconds
    
    private NotificationPoller() {
        this.friendService = new FriendService();
        this.processedNotificationIds = new HashSet<>();
    }
    
    public static NotificationPoller getInstance() {
        if (instance == null) {
            instance = new NotificationPoller();
        }
        return instance;
    }
    
    /**
     * Set callback for when notification is received
     */
    public void setOnNotificationReceived(Consumer<FriendNotificationDTO> callback) {
        this.onNotificationReceived = callback;
    }
    
    /**
     * Start polling for notifications
     */
    public void startPolling() {
        if (isRunning) {
            System.out.println("[NotificationPoller] Already running");
            return;
        }
        
        System.out.println("[NotificationPoller] Starting notification polling...");
        isRunning = true;
        
        pollingTimer = new Timer(true); // Daemon thread
        pollingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkNotifications();
            }
        }, 0, POLLING_INTERVAL); // Check immediately, then every 5 seconds
    }
    
    /**
     * Stop polling
     */
    public void stopPolling() {
        if (pollingTimer != null) {
            pollingTimer.cancel();
            pollingTimer = null;
            isRunning = false;
            System.out.println("[NotificationPoller] Stopped notification polling");
        }
    }
    
    /**
     * Check for new notifications
     */
    private void checkNotifications() {
        try {
            List<FriendNotificationDTO> notifications = friendService.getNotifications();
            
            for (FriendNotificationDTO notification : notifications) {
                // Only process new notifications
                if (!processedNotificationIds.contains(notification.getId())) {
                    processedNotificationIds.add(notification.getId());
                    
                    // Trigger callback on JavaFX thread
                    if (onNotificationReceived != null) {
                        Platform.runLater(() -> {
                            onNotificationReceived.accept(notification);
                            
                            // Mark as read after displaying
                            new Thread(() -> {
                                friendService.markNotificationAsRead(notification.getId());
                            }).start();
                        });
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[NotificationPoller] ❌ Error checking notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear processed notifications (for testing)
     */
    public void clearProcessedNotifications() {
        processedNotificationIds.clear();
    }
}
