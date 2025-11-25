package com.example.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Simple Profile Controller
 * Basic info: avatar, email, username, password, logout
 */
public class ProfileScene {

    // ===== FXML Components =====
    @FXML private Circle avatarCircle;
    @FXML private Label usernameLabel;
    @FXML private Label statusLabel;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;

    // Mock user data
    private static class UserData {
        static String username = "Minh Anh";
        static String email = "minhanh@gmail.com";
        static boolean isOnline = true;
    }

    @FXML
    public void initialize() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        usernameLabel.setText(UserData.username);
        emailField.setText(UserData.email);
        usernameField.setText(UserData.username);
        statusLabel.setText(UserData.isOnline ? "● Online" : "○ Offline");
    }

    // ===== ACTION HANDLERS =====

    @FXML
    private void handleChangeAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) avatarCircle.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            showAlert("Success", "Avatar will be updated to: " + file.getName(), Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void handleEditEmail() {
        emailField.setEditable(true);
        emailField.requestFocus();

        emailField.setOnAction(e -> {
            String newEmail = emailField.getText().trim();
            if (newEmail.contains("@")) {
                UserData.email = newEmail;
                emailField.setEditable(false);
                showAlert("Success", "Email updated!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Invalid email!", Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void handleEditUsername() {
        usernameField.setEditable(true);
        usernameField.requestFocus();

        usernameField.setOnAction(e -> {
            String newUsername = usernameField.getText().trim();
            if (!newUsername.isEmpty()) {
                UserData.username = newUsername;
                usernameLabel.setText(newUsername);
                usernameField.setEditable(false);
                showAlert("Success", "Username updated!", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void handleChangePassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Enter your passwords");

        ButtonType changeBtn = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeBtn, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20px;");

        PasswordField currentPwd = new PasswordField();
        currentPwd.setPromptText("Current password");
        currentPwd.setStyle("-fx-font-size: 14px; -fx-pref-width: 300px;");

        PasswordField newPwd = new PasswordField();
        newPwd.setPromptText("New password");
        newPwd.setStyle("-fx-font-size: 14px; -fx-pref-width: 300px;");

        PasswordField confirmPwd = new PasswordField();
        confirmPwd.setPromptText("Confirm new password");
        confirmPwd.setStyle("-fx-font-size: 14px; -fx-pref-width: 300px;");

        content.getChildren().addAll(
                new Label("Current Password:"), currentPwd,
                new Label("New Password:"), newPwd,
                new Label("Confirm Password:"), confirmPwd
        );

        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result == changeBtn) {
                if (newPwd.getText().equals(confirmPwd.getText()) && !newPwd.getText().isEmpty()) {
                    showAlert("Success", "Password changed!", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Passwords do not match!", Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Are you sure you want to logout?");
        confirm.setContentText("You will be returned to the login screen.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                com.example.util.SceneManager.setTitle("Discord Mini - Login");
                com.example.util.SceneManager.loadContent("content/login-content.fxml");
            }
        });
    }

    @FXML
    private void handleBackToChat() {
        com.example.util.SceneManager.setTitle("Discord Mini - Chat");
        com.example.util.SceneManager.loadContent("chat.fxml");
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}