package com.example.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Toast notification utility for displaying temporary messages
 */
public class NotificationToast {
    
    private static NotificationToast instance;
    private VBox toastContainer;
    
    private NotificationToast() {
    }
    
    public static NotificationToast getInstance() {
        if (instance == null) {
            instance = new NotificationToast();
        }
        return instance;
    }
    
    /**
     * Set the container where toasts will be displayed
     */
    public void setContainer(VBox container) {
        this.toastContainer = container;
    }
    
    /**
     * Show success toast (green)
     */
    public void showSuccess(String message) {
        showToast(message, "success");
    }
    
    /**
     * Show error toast (red)
     */
    public void showError(String message) {
        showToast(message, "error");
    }
    
    /**
     * Show info toast (blue)
     */
    public void showInfo(String message) {
        showToast(message, "info");
    }
    
    /**
     * Show warning toast (yellow)
     */
    public void showWarning(String message) {
        showToast(message, "warning");
    }
    
    /**
     * Show toast notification
     */
    private void showToast(String message, String type) {
        if (toastContainer == null) {
            System.err.println("[NotificationToast] Container not set!");
            return;
        }
        
        Platform.runLater(() -> {
            // Create toast
            HBox toast = new HBox(12);
            toast.setAlignment(Pos.CENTER_LEFT);
            toast.getStyleClass().addAll("notification-toast", "toast-" + type);
            toast.setMaxWidth(400);
            
            // Icon based on type
            String icon = switch (type) {
                case "success" -> "✓";
                case "error" -> "✗";
                case "warning" -> "⚠";
                default -> "ℹ";
            };
            
            Label iconLabel = new Label(icon);
            iconLabel.getStyleClass().add("toast-icon");
            
            Label messageLabel = new Label(message);
            messageLabel.getStyleClass().add("toast-message");
            messageLabel.setWrapText(true);
            
            toast.getChildren().addAll(iconLabel, messageLabel);
            
            // Add to container
            toastContainer.getChildren().add(toast);
            
            // Fade in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
            // Auto hide after 3 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> {
                // Fade out animation
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(ev -> toastContainer.getChildren().remove(toast));
                fadeOut.play();
            });
            pause.play();
        });
    }
}
