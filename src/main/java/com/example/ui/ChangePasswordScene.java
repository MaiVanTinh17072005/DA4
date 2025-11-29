package com.example.ui;

import com.example.api.dto.ResetPasswordResponse;
import com.example.service.AuthService;
import com.example.util.SceneManager;
import com.example.util.ValidationUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.concurrent.CompletableFuture;

/**
 * Controller for Change Password Scene
 * Handles password reset after OTP verification
 */
public class ChangePasswordScene {

    // ===== FXML Components =====
    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordTextField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private Button submitButton;
    @FXML private Button newPasswordToggleButton;
    @FXML private Button confirmPasswordToggleButton;
    @FXML private Hyperlink backToLoginLink;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Text titleText;
    @FXML private Text descriptionText;

    // ===== State =====
    private String email;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    /**
     * Initialize controller - called after FXML is loaded
     */
    @FXML
    public void initialize() {
        setupEnterKeyHandlers();
        setupPasswordVisibilitySync();
        hideError();
    }

    /**
     * Set email for this session
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Setup Enter key to submit form
     */
    private void setupEnterKeyHandlers() {
        newPasswordField.setOnAction(e -> handleSubmit());
        if (newPasswordTextField != null) {
            newPasswordTextField.setOnAction(e -> handleSubmit());
        }
        confirmPasswordField.setOnAction(e -> handleSubmit());
        if (confirmPasswordTextField != null) {
            confirmPasswordTextField.setOnAction(e -> handleSubmit());
        }
    }
    
    /**
     * Sync password between PasswordField and TextField
     */
    private void setupPasswordVisibilitySync() {
        if (newPasswordTextField != null && newPasswordField != null) {
            newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isNewPasswordVisible) {
                    newPasswordTextField.setText(newVal);
                }
            });
            
            newPasswordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isNewPasswordVisible) {
                    newPasswordField.setText(newVal);
                }
            });
        }
        
        if (confirmPasswordTextField != null && confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isConfirmPasswordVisible) {
                    confirmPasswordTextField.setText(newVal);
                }
            });
            
            confirmPasswordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isConfirmPasswordVisible) {
                    confirmPasswordField.setText(newVal);
                }
            });
        }
    }
    
    /**
     * Toggle new password visibility
     */
    @FXML
    private void toggleNewPasswordVisibility() {
        if (newPasswordField == null || newPasswordTextField == null) return;
        
        isNewPasswordVisible = !isNewPasswordVisible;
        
        if (isNewPasswordVisible) {
            newPasswordTextField.setText(newPasswordField.getText());
            newPasswordField.setVisible(false);
            newPasswordField.setManaged(false);
            newPasswordTextField.setVisible(true);
            newPasswordTextField.setManaged(true);
            newPasswordToggleButton.setText("🙈");
        } else {
            newPasswordField.setText(newPasswordTextField.getText());
            newPasswordTextField.setVisible(false);
            newPasswordTextField.setManaged(false);
            newPasswordField.setVisible(true);
            newPasswordField.setManaged(true);
            newPasswordToggleButton.setText("👁");
        }
    }
    
    /**
     * Toggle confirm password visibility
     */
    @FXML
    private void toggleConfirmPasswordVisibility() {
        if (confirmPasswordField == null || confirmPasswordTextField == null) return;
        
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        
        if (isConfirmPasswordVisible) {
            confirmPasswordTextField.setText(confirmPasswordField.getText());
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            confirmPasswordTextField.setVisible(true);
            confirmPasswordTextField.setManaged(true);
            confirmPasswordToggleButton.setText("🙈");
        } else {
            confirmPasswordField.setText(confirmPasswordTextField.getText());
            confirmPasswordTextField.setVisible(false);
            confirmPasswordTextField.setManaged(false);
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            confirmPasswordToggleButton.setText("👁");
        }
    }

    // ===== Action Handlers =====

    /**
     * Handle submit button click - change password
     */
    @FXML
    private void handleSubmit() {
        String newPassword = isNewPasswordVisible && newPasswordTextField != null
            ? newPasswordTextField.getText()
            : newPasswordField.getText();
        String confirmPassword = isConfirmPasswordVisible && confirmPasswordTextField != null
            ? confirmPasswordTextField.getText()
            : confirmPasswordField.getText();

        // Validation
        if (!validateInput(newPassword, confirmPassword)) {
            return;
        }

        // Show loading state
        setLoading(true);
        hideError();

        // Perform async password change
        CompletableFuture.runAsync(() -> {
            try {
                performPasswordChange(newPassword);
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    /**
     * Navigate back to login scene
     */
    @FXML
    private void handleBackToLogin() {
        // Swap content using SceneManager
        SceneManager.setTitle("Discord Mini - Login");
        SceneManager.loadContent("content/login-content.fxml");
    }

    // ===== Business Logic =====

    /**
     * Validate user input using ValidationUtil
     */
    private boolean validateInput(String newPassword, String confirmPassword) {
        // Password strength validation
        ValidationUtil.ValidationResult passwordResult = ValidationUtil.validatePassword(newPassword);
        if (!passwordResult.isValid()) {
            showError(passwordResult.getErrorMessage());
            return false;
        }
        
        // Password match validation
        ValidationUtil.ValidationResult passwordMatchResult = ValidationUtil.validatePasswordMatch(newPassword, confirmPassword);
        if (!passwordMatchResult.isValid()) {
            showError(passwordMatchResult.getErrorMessage());
            return false;
        }

        return true;
    }

    /**
     * Perform password change using AuthService
     */
    private void performPasswordChange(String newPassword) {
        try {
            System.out.println("[ChangePassword] Resetting password for email: " + email);
            
            // Call the API through AuthService
            AuthService authService = AuthService.getInstance();
            ResetPasswordResponse response = authService.resetPassword(email, newPassword);
            
            System.out.println("[ChangePassword] Response received: " + response);
            
            // Handle response on UI thread
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    // Success - show message and navigate to login
                    System.out.println("[ChangePassword] Success! Navigating to login");
                    showSuccess("Mật khẩu đã được đổi thành công! Đang chuyển đến đăng nhập...");
                    
                    // Navigate to login after 2 seconds
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            Platform.runLater(() -> handleBackToLogin());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                } else {
                    // Server returned error message
                    System.out.println("[ChangePassword] Error from server: " + response.getMessage());
                    showError(response.getMessage() != null ? response.getMessage() : "Không thể đổi mật khẩu. Vui lòng thử lại.");
                }
            });
        } catch (java.net.SocketTimeoutException e) {
            // Timeout error
            System.err.println("[ChangePassword] Timeout error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Yêu cầu mất quá nhiều thời gian. Vui lòng thử lại.");
            });
        } catch (java.net.ConnectException e) {
            // Server not running
            System.err.println("[ChangePassword] Connection error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Không thể kết nối đến server.\nVui lòng kiểm tra server đã chạy chưa.");
            });
        } catch (Exception e) {
            // Network or other error
            System.err.println("[ChangePassword] General error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Lỗi kết nối: " + e.getMessage());
            });
        }
    }

    /**
     * Handle errors
     */
    private void handleError(Exception e) {
        Platform.runLater(() -> {
            setLoading(false);
            showError("Connection error: " + e.getMessage());
        });
    }

    // ===== UI State Management =====

    /**
     * Set loading state (disable inputs, show spinner)
     */
    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
        submitButton.setDisable(loading);
        newPasswordField.setDisable(loading);
        if (newPasswordTextField != null) {
            newPasswordTextField.setDisable(loading);
        }
        confirmPasswordField.setDisable(loading);
        if (confirmPasswordTextField != null) {
            confirmPasswordTextField.setDisable(loading);
        }
        if (newPasswordToggleButton != null) {
            newPasswordToggleButton.setDisable(loading);
        }
        if (confirmPasswordToggleButton != null) {
            confirmPasswordToggleButton.setDisable(loading);
        }
        backToLoginLink.setDisable(loading);
    }

    // ===== UI Feedback Methods =====

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setStyle("-fx-text-fill: #f04747;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        errorLabel.setText("✓ " + message);
        errorLabel.setStyle("-fx-text-fill: #43b581;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /**
     * Hide error/success message
     */
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    // ===== Utility Methods =====

    /**
     * Simulate network delay (for testing)
     */
    private void simulateNetworkDelay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

