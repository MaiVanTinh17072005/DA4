package com.example.ui;

import com.example.util.ValidationUtil;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

/**
 * Profile Controller with Validation
 * Manages user profile: avatar, email, username, password
 * Uses ValidationUtil for comprehensive input validation
 */
public class ProfileScene {

    // ===== FXML Components =====
    @FXML
    private Circle avatarCircle;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private Label usernameLabel;
    
    @FXML
    private HBox notificationBar;
    
    @FXML
    private Label notificationIcon;
    
    @FXML
    private Label notificationMessage;
    
    @FXML
    private Button saveButton;
    
    private PauseTransition hideNotificationTimer;
    
    // Track if data has been modified
    private boolean isModified = false;
    private String originalEmail;
    private String originalUsername;

    // ===== User Data =====
    private static class UserData {
        static String email = "minhanh@gmail.com";
        static String username = "Minh Anh";
    }

    @FXML
    public void initialize() {
        // Initialize fields with current user data
        if (emailField != null) {
            emailField.setText(UserData.email);
            originalEmail = UserData.email;
        }
        if (usernameField != null) {
            usernameField.setText(UserData.username);
            originalUsername = UserData.username;
        }
        if (usernameLabel != null) {
            usernameLabel.setText(UserData.username);
        }
        
        // Initialize auto-hide timer for notifications
        hideNotificationTimer = new PauseTransition(Duration.seconds(5));
        hideNotificationTimer.setOnFinished(e -> hideNotification());
    }

    // ===== Event Handlers =====
    @FXML
    private void handleChangeAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn Ảnh Đại Diện");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) avatarCircle.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            showAlert("Thành Công", "Ảnh đại diện sẽ được cập nhật: " + file.getName(), Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void handleEditEmail() {
        emailField.setEditable(true);
        emailField.requestFocus();
        emailField.selectAll();
        
        // Show save button when editing
        showSaveButton();
        
        // Track changes
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            checkIfModified();
        });
    }

    @FXML
    private void handleEditUsername() {
        usernameField.setEditable(true);
        usernameField.requestFocus();
        usernameField.selectAll();
        
        // Show save button when editing
        showSaveButton();
        
        // Track changes
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            checkIfModified();
        });
    }
    
    @FXML
    private void handleSaveChanges() {
        String newEmail = emailField.getText().trim();
        String newUsername = usernameField.getText();
        
        // Validate email
        ValidationUtil.ValidationResult emailResult = ValidationUtil.validateEmail(newEmail);
        if (!emailResult.isValid()) {
            showNotification("⚠", "Email: " + emailResult.getErrorMessage(), false);
            emailField.requestFocus();
            return;
        }
        
        // Validate username
        ValidationUtil.ValidationResult usernameResult = ValidationUtil.validateUsername(newUsername);
        if (!usernameResult.isValid()) {
            showNotification("⚠", "Username: " + usernameResult.getErrorMessage(), false);
            usernameField.requestFocus();
            return;
        }
        
        // Both valid, save changes
        UserData.email = newEmail;
        UserData.username = newUsername;
        usernameLabel.setText(newUsername);
        
        // Disable editing
        emailField.setEditable(false);
        usernameField.setEditable(false);
        
        // Update original values
        originalEmail = newEmail;
        originalUsername = newUsername;
        
        // Hide save button
        hideSaveButton();
        isModified = false;
        
        // Show success notification
        showNotification("✓", "Thông tin đã được lưu thành công!", true);
    }

    @FXML
    private void handleChangePassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đổi Mật Khẩu");
        dialog.setHeaderText("Nhập mật khẩu mới của bạn");

        ButtonType changeBtn = new ButtonType("Đổi Mật Khẩu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeBtn, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20px;");

        PasswordField currentPwd = new PasswordField();
        currentPwd.setPromptText("Mật khẩu hiện tại");
        currentPwd.setStyle("-fx-font-size: 14px; -fx-pref-width: 300px;");

        PasswordField newPwd = new PasswordField();
        newPwd.setPromptText("Mật khẩu mới (ít nhất 6 ký tự, có chữ hoa, chữ thường, số, ký tự đặc biệt)");
        newPwd.setStyle("-fx-font-size: 14px; -fx-pref-width: 300px;");

        PasswordField confirmPwd = new PasswordField();
        confirmPwd.setPromptText("Xác nhận mật khẩu mới");
        confirmPwd.setStyle("-fx-font-size: 14px; -fx-pref-width: 300px;");

        Label requirementsLabel = new Label(
            "Yêu cầu mật khẩu:\n" +
            "• Ít nhất 6 ký tự\n" +
            "• Có chữ cái in hoa (A-Z)\n" +
            "• Có chữ cái thường (a-z)\n" +
            "• Có chữ số (0-9)\n" +
            "• Có ký tự đặc biệt (!@#$%^&*...)"
        );
        requirementsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-padding: 10px 0;");

        content.getChildren().addAll(
                new Label("Mật Khẩu Hiện Tại:"), currentPwd,
                new Label("Mật Khẩu Mới:"), newPwd,
                new Label("Xác Nhận Mật Khẩu:"), confirmPwd,
                requirementsLabel
        );

        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result == changeBtn) {
                String currentPassword = currentPwd.getText();
                String newPassword = newPwd.getText();
                String confirmPassword = confirmPwd.getText();
                
                // Validate current password is not empty
                if (currentPassword == null || currentPassword.trim().isEmpty()) {
                    showAlert("Lỗi", "Vui lòng nhập mật khẩu hiện tại", Alert.AlertType.ERROR);
                    return;
                }
                
                // Validate new password using ValidationUtil
                ValidationUtil.ValidationResult passwordResult = ValidationUtil.validatePassword(newPassword);
                if (!passwordResult.isValid()) {
                    showAlert("Lỗi Validation Mật Khẩu", passwordResult.getErrorMessage(), Alert.AlertType.ERROR);
                    return;
                }
                
                // Validate password match using ValidationUtil
                ValidationUtil.ValidationResult matchResult = ValidationUtil.validatePasswordMatch(newPassword, confirmPassword);
                if (!matchResult.isValid()) {
                    showAlert("Lỗi Xác Nhận", matchResult.getErrorMessage(), Alert.AlertType.ERROR);
                    return;
                }
                
                // All validations passed
                showAlert("Thành Công", 
                    "Mật khẩu đã được thay đổi thành công!\n" +
                    "Mật khẩu mới của bạn đáp ứng tất cả yêu cầu bảo mật.", 
                    Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng Xuất");
        confirm.setHeaderText("Bạn có chắc muốn đăng xuất?");
        confirm.setContentText("Bạn sẽ được chuyển về màn hình đăng nhập.");

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
        com.example.util.SceneManager.loadContent("content/chat-content.fxml");
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show inline notification bar
     * @param icon Icon to display (e.g., "✓" for success, "⚠" for error)
     * @param message Message to display
     * @param isSuccess true for success (green), false for error (red)
     */
    private void showNotification(String icon, String message, boolean isSuccess) {
        if (notificationBar == null) return;
        
        // Set icon and message
        notificationIcon.setText(icon);
        notificationMessage.setText(message);
        
        // Remove previous style classes
        notificationBar.getStyleClass().removeAll("success", "error");
        
        // Add appropriate style class
        if (isSuccess) {
            notificationBar.getStyleClass().add("success");
        } else {
            notificationBar.getStyleClass().add("error");
        }
        
        // Show notification
        notificationBar.setVisible(true);
        notificationBar.setManaged(true);
        
        // Reset and start auto-hide timer
        hideNotificationTimer.stop();
        hideNotificationTimer.playFromStart();
    }
    
    /**
     * Hide notification bar
     */
    private void hideNotification() {
        if (notificationBar != null) {
            notificationBar.setVisible(false);
            notificationBar.setManaged(false);
        }
    }
    
    /**
     * Handle close notification button click
     */
    @FXML
    private void handleCloseNotification() {
        hideNotificationTimer.stop();
        hideNotification();
    }
    
    /**
     * Show save button
     */
    private void showSaveButton() {
        if (saveButton != null) {
            saveButton.setVisible(true);
            saveButton.setManaged(true);
        }
    }
    
    /**
     * Hide save button
     */
    private void hideSaveButton() {
        if (saveButton != null) {
            saveButton.setVisible(false);
            saveButton.setManaged(false);
        }
    }
    
    /**
     * Check if data has been modified
     */
    private void checkIfModified() {
        String currentEmail = emailField.getText().trim();
        String currentUsername = usernameField.getText();
        
        isModified = !currentEmail.equals(originalEmail) || !currentUsername.equals(originalUsername);
        
        // Show/hide save button based on modification status
        if (isModified) {
            showSaveButton();
        }
    }
}