package com.example.ui;

import com.example.util.SceneManager;
import javafx.fxml.FXML;

/**
 * Controller for the shared sidebar navigation
 */
public class SidebarController {

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
        SceneManager.setTitle("Discord Mini - Đăng nhập");
        SceneManager.loadContent("content/login-content.fxml");
    }
}
