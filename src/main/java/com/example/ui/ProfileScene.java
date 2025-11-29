package com.example.ui;

import com.example.api.dto.UserDTO;
import com.example.util.SessionManager;
import com.example.util.ValidationUtil;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

/**
 * Profile Controller with Real-Time Validation
 * Manages user profile: avatar, email, username, password
 * Uses ValidationUtil for comprehensive input validation
 * Loads user data from SessionManager (Redis cache)
 * Validates email and username in real-time as user types
 * Loads default avatar image from resources
 * Save button is always visible
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
    private PauseTransition validationDelayTimer;
    
    // Track if data has been modified
    private boolean isModified = false;
    private String originalEmail;
    private String originalUsername;
    
    // Track validation state
    private boolean isEmailValid = true;
    private boolean isUsernameValid = true;

    // ===== User Data (Fallback) =====
    private static class UserData {
        static String email = "minhanh@gmail.com";
        static String username = "Minh Anh";
    }

    @FXML
    public void initialize() {
        System.out.println("[ProfileScene] Initializing profile page...");
        
        // Load user data from SessionManager (data from Redis cache via login)
        UserDTO currentUser = SessionManager.getCurrentUser();
        
        if (currentUser != null) {
            System.out.println("[ProfileScene] Loading user data from session:");
            System.out.println("  - User ID: " + currentUser.getId());
            System.out.println("  - Email: " + currentUser.getEmail());
            System.out.println("  - Username: " + currentUser.getUsername());
            System.out.println("  - Status: " + currentUser.getStatus());
            
            // Initialize fields with current user data from session
            if (emailField != null) {
                emailField.setText(currentUser.getEmail());
                originalEmail = currentUser.getEmail();
            }
            if (usernameField != null) {
                usernameField.setText(currentUser.getUsername());
                originalUsername = currentUser.getUsername();
            }
            if (usernameLabel != null) {
                usernameLabel.setText(currentUser.getUsername());
            }
        } else {
            System.out.println("[ProfileScene] ⚠ No user session found, using default values");
            // Fallback to default values if no session exists
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
        }
        
        // Load default avatar image
        loadDefaultAvatar();
        
        // Initialize auto-hide timer for notifications
        hideNotificationTimer = new PauseTransition(Duration.seconds(5));
        hideNotificationTimer.setOnFinished(e -> hideNotification());
        
        // Initialize validation delay timer (wait 500ms after user stops typing)
        validationDelayTimer = new PauseTransition(Duration.millis(500));
        
        System.out.println("[ProfileScene] Profile page initialized successfully");
    }
    
    /**
     * Load default avatar image
     */
    private void loadDefaultAvatar() {
        try {
            // Load default avatar image from resources
            String imagePath = "/com/example/images/macdinh.jpg";
            Image defaultAvatar = new Image(getClass().getResourceAsStream(imagePath));
            
            if (defaultAvatar != null && !defaultAvatar.isError()) {
                // Set image as fill pattern for the circle
                avatarCircle.setFill(new ImagePattern(defaultAvatar));
                System.out.println("[ProfileScene] ✓ Default avatar loaded successfully");
            } else {
                System.out.println("[ProfileScene] ⚠ Failed to load default avatar image");
            }
        } catch (Exception e) {
            System.out.println("[ProfileScene] ❌ Error loading default avatar: " + e.getMessage());
            e.printStackTrace();
        }
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
            try {
                // Load selected image
                Image newAvatar = new Image(file.toURI().toString());
                avatarCircle.setFill(new ImagePattern(newAvatar));
                showAlert("Thành Công", "Ảnh đại diện đã được cập nhật: " + file.getName(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Lỗi", "Không thể tải ảnh: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleEditEmail() {
        emailField.setEditable(true);
        emailField.requestFocus();
        emailField.selectAll();
        
        // Track changes and validate in real-time
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            checkIfModified();
            
            // Delay validation to avoid validating on every keystroke
            validationDelayTimer.stop();
            validationDelayTimer.setOnFinished(e -> validateEmailField(newVal));
            validationDelayTimer.playFromStart();
        });
    }

    @FXML
    private void handleEditUsername() {
        usernameField.setEditable(true);
        usernameField.requestFocus();
        usernameField.selectAll();
        
        // Track changes and validate in real-time
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            checkIfModified();
            
            // Delay validation to avoid validating on every keystroke
            validationDelayTimer.stop();
            validationDelayTimer.setOnFinished(e -> validateUsernameField(newVal));
            validationDelayTimer.playFromStart();
        });
    }
    
    /**
     * Validate email field in real-time
     */
    private void validateEmailField(String email) {
        if (email == null || email.trim().isEmpty()) {
            // Clear styling if empty
            emailField.setStyle("");
            isEmailValid = false;
            return;
        }
        
        ValidationUtil.ValidationResult result = ValidationUtil.validateEmail(email.trim());
        if (!result.isValid()) {
            // Show validation error with red border
            emailField.setStyle("-fx-border-color: #f04747; -fx-border-width: 2px;");
            showNotification("⚠", "Email: " + result.getErrorMessage(), false);
            isEmailValid = false;
        } else {
            // Clear error styling with green border
            emailField.setStyle("-fx-border-color: #43b581; -fx-border-width: 2px;");
            hideNotification();
            isEmailValid = true;
        }
        
        // Update save button state
        updateSaveButtonState();
    }
    
    /**
     * Validate username field in real-time
     */
    private void validateUsernameField(String username) {
        if (username == null || username.trim().isEmpty()) {
            // Clear styling if empty
            usernameField.setStyle("");
            isUsernameValid = false;
            return;
        }
        
        ValidationUtil.ValidationResult result = ValidationUtil.validateUsername(username);
        if (!result.isValid()) {
            // Show validation error with red border
            usernameField.setStyle("-fx-border-color: #f04747; -fx-border-width: 2px;");
            showNotification("⚠", "Username: " + result.getErrorMessage(), false);
            isUsernameValid = false;
        } else {
            // Clear error styling with green border
            usernameField.setStyle("-fx-border-color: #43b581; -fx-border-width: 2px;");
            hideNotification();
            isUsernameValid = true;
        }
        
        // Update save button state
        updateSaveButtonState();
    }
    
    /**
     * Update save button enabled/disabled state based on validation
     */
    private void updateSaveButtonState() {
        if (saveButton != null) {
            // Enable save button only if both fields are valid and modified
            saveButton.setDisable(!isEmailValid || !isUsernameValid || !isModified);
        }
    }
    
    @FXML
    private void handleSaveChanges() {
        String newEmail = emailField.getText().trim();
        String newUsername = usernameField.getText();
        
        // Final validation before saving
        ValidationUtil.ValidationResult emailResult = ValidationUtil.validateEmail(newEmail);
        if (!emailResult.isValid()) {
            showNotification("⚠", "Email: " + emailResult.getErrorMessage(), false);
            emailField.requestFocus();
            return;
        }
        
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
        
        // Disable editing and clear styling
        emailField.setEditable(false);
        emailField.setStyle("");
        usernameField.setEditable(false);
        usernameField.setStyle("");
        
        // Update original values
        originalEmail = newEmail;
        originalUsername = newUsername;
        
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
     * Check if data has been modified
     */
    private void checkIfModified() {
        String currentEmail = emailField.getText().trim();
        String currentUsername = usernameField.getText();
        
        isModified = !currentEmail.equals(originalEmail) || !currentUsername.equals(originalUsername);
        
        // Update save button state
        updateSaveButtonState();
    }
}