package com.example.util;

import com.example.api.dto.PendingFriendRequestDTO;
import com.example.service.FriendService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.animation.FadeTransition;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Notification Badge Manager
 * Manages the notification badge for pending friend requests
 */
public class NotificationBadge {
    
    private static NotificationBadge instance;
    private final FriendService friendService;
    private final ScheduledExecutorService scheduler;
    private StackPane badgeContainer;
    private Label badgeLabel;
    private Circle badgeCircle;
    private int pendingCount = 0;
    
    private NotificationBadge() {
        this.friendService = new FriendService();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("NotificationBadge-Scheduler");
            return thread;
        });
    }
    
    public static NotificationBadge getInstance() {
        if (instance == null) {
            instance = new NotificationBadge();
        }
        return instance;
    }
    
    /**
     * Create badge UI component
     * @return StackPane containing the badge
     */
    public StackPane createBadge() {
        badgeContainer = new StackPane();
        badgeContainer.setVisible(false);
        badgeContainer.setManaged(false);
        
        // Red circle background
        badgeCircle = new Circle(10);
        badgeCircle.setFill(Color.web("#ED4245")); // Discord red
        badgeCircle.setStroke(Color.WHITE);
        badgeCircle.setStrokeWidth(2);
        
        // Count label
        badgeLabel = new Label("0");
        badgeLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 10px; " +
            "-fx-font-weight: bold;"
        );
        
        badgeContainer.getChildren().addAll(badgeCircle, badgeLabel);
        badgeContainer.setAlignment(Pos.CENTER);
        
        return badgeContainer;
    }
    
    /**
     * Update badge count
     * @param count Number of pending requests
     */
    public void updateCount(int count) {
        this.pendingCount = count;
        
        Platform.runLater(() -> {
            if (count > 0) {
                badgeLabel.setText(String.valueOf(count));
                badgeContainer.setVisible(true);
                badgeContainer.setManaged(true);
                
                // Animate badge appearance
                FadeTransition fade = new FadeTransition(Duration.millis(300), badgeContainer);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);
                fade.play();
            } else {
                badgeContainer.setVisible(false);
                badgeContainer.setManaged(false);
            }
        });
    }
    
    /**
     * Start polling for pending requests
     * Polls every 5 seconds
     */
    public void startPolling() {
        // Initial fetch
        fetchPendingCount();
        
        // Schedule periodic polling
        scheduler.scheduleAtFixedRate(() -> {
            try {
                fetchPendingCount();
            } catch (Exception e) {
                System.err.println("❌ [NotificationBadge] Error polling pending requests: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
        
        System.out.println("✅ [NotificationBadge] Started polling for pending requests");
    }
    
    /**
     * Stop polling
     */
    public void stopPolling() {
        scheduler.shutdown();
        System.out.println("✅ [NotificationBadge] Stopped polling");
    }
    
    /**
     * Fetch pending request count from server
     */
    private void fetchPendingCount() {
        // Don't fetch if user is not logged in
        if (!com.example.util.SessionManager.isLoggedIn()) {
            return;
        }
        
        List<PendingFriendRequestDTO> requests = friendService.getPendingRequests();
        updateCount(requests.size());
    }
    
    /**
     * Manually refresh the badge
     */
    public void refresh() {
        fetchPendingCount();
    }
    
    /**
     * Get current pending count
     */
    public int getPendingCount() {
        return pendingCount;
    }
}
