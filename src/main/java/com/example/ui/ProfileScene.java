package com.example.ui;

import com.example.api.dto.ChangePasswordRequest;
import com.example.api.dto.ChangePasswordResponse;
import com.example.api.dto.UpdateProfileRequest;
import com.example.api.dto.UpdateProfileResponse;
import com.example.api.dto.UserDTO;
import com.example.crypto.PasswordHashUtil;
import com.example.service.UserService;
import com.example.util.SessionManager;
import com.example.util.ValidationUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Profile Controller with Real-Time Validation and API Integration
 * Manages user profile: avatar, email, username, password
 * Saves avatar locally and sends URL to server
 * Updates database and Redis cache via API
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
    
    // Avatar management
    private String currentAvatarUrl = "/com/example/images/macdinh.jpg"; // Default avatar
    
    // Services
    private final UserService userService = new UserService();

    // ===== User Data (Fallback) =====
    private static class UserData {
        static String email = "minhanh@gmail.com";
        static String username = "Minh Anh";
    }

    @FXML
    public void initialize() {
        System.out.println("[ProfileScene] Initializing profile page...");
        
        // Load user data from SessionManager
        UserDTO currentUser = SessionManager.getCurrentUser();
        
        if (currentUser != null) {
            System.out.println("[ProfileScene] Loading user data from session:");
            System.out.println("  - User ID: " + currentUser.getId());
            System.out.println("  - Email: " + currentUser.getEmail());
            System.out.println("  - Username: " + currentUser.getUsername());
            
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
            
            // Load avatar if user has one
            if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                currentAvatarUrl = currentUser.getAvatarUrl();
            }
        } else {
            System.out.println("[ProfileScene] ⚠ No user session found, using default values");
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
        
        // Initialize timers
        hideNotificationTimer = new PauseTransition(Duration.seconds(5));
        hideNotificationTimer.setOnFinished(e -> hideNotification());
        
        validationDelayTimer = new PauseTransition(Duration.millis(500));
        
        System.out.println("[ProfileScene] Profile page initialized successfully");
    }
    
    private void loadDefaultAvatar() {
        try {
            Image defaultAvatar = new Image(getClass().getResourceAsStream(currentAvatarUrl));
            if (defaultAvatar != null && !defaultAvatar.isError()) {
                avatarCircle.setFill(new ImagePattern(defaultAvatar));
                System.out.println("[ProfileScene] ✓ Avatar loaded: " + currentAvatarUrl);
            }
        } catch (Exception e) {
            System.out.println("[ProfileScene] ❌ Error loading avatar: " + e.getMessage());
        }
    }

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
                // Generate unique filename
                String timestamp = String.valueOf(System.currentTimeMillis());
                String extension = getFileExtension(file.getName());
                String newFileName = "avatar_" + timestamp + extension;
                
                // Get resources/images directory path
                String resourcesPath = "src/main/resources/com/example/images/";
                Path targetPath = Paths.get(resourcesPath + newFileName);
                
                // Create directory if not exists
                Files.createDirectories(targetPath.getParent());
                
                // Copy file to resources/images
                Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[ProfileScene] ✓ Avatar saved to: " + targetPath.toAbsolutePath());
                
                // Update avatar URL (relative path for server)
                currentAvatarUrl = "/com/example/images/" + newFileName;
                System.out.println("[ProfileScene] Avatar URL: " + currentAvatarUrl);
                
                // Load and display new avatar
                Image newAvatar = new Image(file.toURI().toString());
                avatarCircle.setFill(new ImagePattern(newAvatar));
                
                // Mark as modified
                isModified = true;
                updateSaveButtonState();
                
                showNotification("✓", "Ảnh đại diện đã được chọn. Nhấn 'Lưu Thay Đổi' để cập nhật.", true);
                
            } catch (IOException e) {
                System.out.println("[ProfileScene] ❌ Error saving avatar: " + e.getMessage());
                e.printStackTrace();
                showAlert("Lỗi", "Không thể lưu ảnh: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return ".jpg";
    }

    @FXML
    private void handleEditEmail() {
        emailField.setEditable(true);
        emailField.requestFocus();
        emailField.selectAll();
        
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            checkIfModified();
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
        
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            checkIfModified();
            validationDelayTimer.stop();
            validationDelayTimer.setOnFinished(e -> validateUsernameField(newVal));
            validationDelayTimer.playFromStart();
        });
    }
    
    private void validateEmailField(String email) {
        if (email == null || email.trim().isEmpty()) {
            emailField.setStyle("");
            isEmailValid = false;
            return;
        }
        
        ValidationUtil.ValidationResult result = ValidationUtil.validateEmail(email.trim());
        if (!result.isValid()) {
            emailField.setStyle("-fx-border-color: #f04747; -fx-border-width: 2px;");
            showNotification("⚠", "Email: " + result.getErrorMessage(), false);
            isEmailValid = false;
        } else {
            emailField.setStyle("-fx-border-color: #43b581; -fx-border-width: 2px;");
            hideNotification();
            isEmailValid = true;
        }
        
        updateSaveButtonState();
    }
    
    private void validateUsernameField(String username) {
        if (username == null || username.trim().isEmpty()) {
            usernameField.setStyle("");
            isUsernameValid = false;
            return;
        }
        
        ValidationUtil.ValidationResult result = ValidationUtil.validateUsername(username);
        if (!result.isValid()) {
            usernameField.setStyle("-fx-border-color: #f04747; -fx-border-width: 2px;");
            showNotification("⚠", "Username: " + result.getErrorMessage(), false);
            isUsernameValid = false;
        } else {
            usernameField.setStyle("-fx-border-color: #43b581; -fx-border-width: 2px;");
            hideNotification();
            isUsernameValid = true;
        }
        
        updateSaveButtonState();
    }
    
    private void updateSaveButtonState() {
        if (saveButton != null) {
            saveButton.setDisable(!isEmailValid || !isUsernameValid || !isModified);
        }
    }
    
    @FXML
    private void handleSaveChanges() {
        String newEmail = emailField.getText().trim();
        String newUsername = usernameField.getText();
        
        // Final validation
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
        
        // Disable button and show loading
        saveButton.setDisable(true);
        saveButton.setText("⏳ Đang lưu...");
        
        // Create request
        UpdateProfileRequest request = new UpdateProfileRequest(newEmail, newUsername, currentAvatarUrl);
        System.out.println("[ProfileScene] Sending profile update: " + request);
        
        // Call API in background
        new Thread(() -> {
            try {
                UpdateProfileResponse response = userService.updateProfile(request);
                
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        System.out.println("[ProfileScene] ✓ Profile updated successfully");
                        
                        // Update SessionManager
                        if (response.getUser() != null) {
                            SessionManager.setCurrentUser(response.getUser());
                        }
                        
                        // Update UI
                        UserData.email = newEmail;
                        UserData.username = newUsername;
                        usernameLabel.setText(newUsername);
                        
                        emailField.setEditable(false);
                        emailField.setStyle("");
                        usernameField.setEditable(false);
                        usernameField.setStyle("");
                        
                        originalEmail = newEmail;
                        originalUsername = newUsername;
                        isModified = false;
                        
                        showNotification("✓", "Thông tin đã được lưu thành công!", true);
                    } else {
                        System.out.println("[ProfileScene] ❌ Update failed: " + response.getMessage());
                        showNotification("⚠", "Lỗi: " + response.getMessage(), false);
                    }
                    
                    saveButton.setDisable(false);
                    saveButton.setText("💾 Lưu Thay Đổi");
                });
                
            } catch (IOException e) {
                System.out.println("[ProfileScene] ❌ Network error: " + e.getMessage());
                
                Platform.runLater(() -> {
                    showNotification("⚠", "Không thể kết nối đến server: " + e.getMessage(), false);
                    saveButton.setDisable(false);
                    saveButton.setText("💾 Lưu Thay Đổi");
                });
            }
        }).start();
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
        newPwd.setPromptText("Mật khẩu mới");
        newPwd.setStyle("-fx-font-size: 14px; -fx-pref-width: 300px;");

        PasswordField confirmPwd = new PasswordField();
        confirmPwd.setPromptText("Xác nhận mật khẩu mới");
        confirmPwd.setStyle("-fx-font-size: 14px; -fx-pref-width: 300px;");

        content.getChildren().addAll(
                new Label("Mật Khẩu Hiện Tại:"), currentPwd,
                new Label("Mật Khẩu Mới:"), newPwd,
                new Label("Xác Nhận Mật Khẩu:"), confirmPwd
        );

        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result == changeBtn) {
                // Validate new password
                ValidationUtil.ValidationResult passwordResult = ValidationUtil.validatePassword(newPwd.getText());
                if (!passwordResult.isValid()) {
                    showAlert("Lỗi", passwordResult.getErrorMessage(), Alert.AlertType.ERROR);
                    return;
                }
                
                // Validate password match
                ValidationUtil.ValidationResult matchResult = ValidationUtil.validatePasswordMatch(newPwd.getText(), confirmPwd.getText());
                if (!matchResult.isValid()) {
                    showAlert("Lỗi", matchResult.getErrorMessage(), Alert.AlertType.ERROR);
                    return;
                }
                
                // Hash passwords with SHA-256
                String hashedCurrent = PasswordHashUtil.hashSHA256(currentPwd.getText());
                String hashedNew = PasswordHashUtil.hashSHA256(newPwd.getText());
                
                // Create request
                ChangePasswordRequest request = new ChangePasswordRequest(hashedCurrent, hashedNew);
                System.out.println("[ProfileScene] Sending change password request");
                
                // Call API in background
                new Thread(() -> {
                    try {
                        ChangePasswordResponse response = userService.changePassword(request);
                        
                        Platform.runLater(() -> {
                            if (response.isSuccess()) {
                                System.out.println("[ProfileScene] ✓ Password changed successfully");
                                showAlert("Thành Công", response.getMessage(), Alert.AlertType.INFORMATION);
                            } else {
                                System.out.println("[ProfileScene] ❌ Change password failed: " + response.getMessage());
                                showAlert("Lỗi", response.getMessage(), Alert.AlertType.ERROR);
                            }
                        });
                        
                    } catch (IOException e) {
                        System.out.println("[ProfileScene] ❌ Network error: " + e.getMessage());
                        Platform.runLater(() -> {
                            showAlert("Lỗi", "Không thể kết nối đến server: " + e.getMessage(), Alert.AlertType.ERROR);
                        });
                    }
                }).start();
            }
        });
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng Xuất");
        confirm.setHeaderText("Bạn có chắc muốn đăng xuất?");

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
    
    private void showNotification(String icon, String message, boolean isSuccess) {
        if (notificationBar == null) return;
        
        notificationIcon.setText(icon);
        notificationMessage.setText(message);
        
        notificationBar.getStyleClass().removeAll("success", "error");
        
        if (isSuccess) {
            notificationBar.getStyleClass().add("success");
        } else {
            notificationBar.getStyleClass().add("error");
        }
        
        notificationBar.setVisible(true);
        notificationBar.setManaged(true);
        
        hideNotificationTimer.stop();
        hideNotificationTimer.playFromStart();
    }
    
    private void hideNotification() {
        if (notificationBar != null) {
            notificationBar.setVisible(false);
            notificationBar.setManaged(false);
        }
    }
    
    @FXML
    private void handleCloseNotification() {
        hideNotificationTimer.stop();
        hideNotification();
    }
    
    private void checkIfModified() {
        String currentEmail = emailField.getText().trim();
        String currentUsername = usernameField.getText();
        
        isModified = !currentEmail.equals(originalEmail) || !currentUsername.equals(originalUsername);
        
        updateSaveButtonState();
    }
}