package com.example.ui;

import com.example.service.AuthService;
import com.example.util.SceneManager;
import com.example.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

/**
 * Controller for the shared sidebar navigation
 */
public class SidebarController {

    private final AuthService authService = AuthService.getInstance();

    @FXML
    private void handleHomeClick() {
        SceneManager.setTitle("Discord Mini - Chat");
        SceneManager.loadContent("content/chat-content.fxml");
    }

    @FXML
    private void handleFriendsClick() {
        SceneManager.setTitle("Discord Mini - Bạn bè");
        SceneManager.loadContent("content/friends-content.fxml");
    }

    @FXML
    private void handleLivestreamClick() {
        // Placeholder for now
        System.out.println("Livestream clicked - Feature in development");
    }

    @FXML
    private void handleLogout() {
        // Perform logout in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                System.out.println("Initiating logout...");
                
                // Call logout API to update status to offline on server
                authService.logout();
                
                // Clear local session
                SessionManager.clearSession();
                
                System.out.println("Logout completed successfully");
                
                // Switch to login screen on JavaFX thread
                Platform.runLater(() -> {
                    SceneManager.setTitle("Discord Mini - Đăng nhập");
                    SceneManager.loadContent("content/login-content.fxml");
                });
                
            } catch (Exception e) {
                System.err.println("Logout error: " + e.getMessage());
                e.printStackTrace();
                
                // Even if server request fails, still clear local session and go to login
                SessionManager.clearSession();
                
                Platform.runLater(() -> {
                    // Show error alert
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Cảnh báo");
                    alert.setHeaderText("Lỗi khi đăng xuất");
                    alert.setContentText("Không thể kết nối với server, nhưng bạn đã được đăng xuất cục bộ.");
                    alert.showAndWait();
                    
                    // Still navigate to login screen
                    SceneManager.setTitle("Discord Mini - Đăng nhập");
                    SceneManager.loadContent("content/login-content.fxml");
                });
            }
        }).start();
    }
}
