package com.example.ui;

import com.example.api.dto.ForgotPasswordResponse;
import com.example.service.AuthService;
import com.example.util.SceneManager;
import com.example.util.ValidationUtil;
import com.example.util.ValidationUtil.ValidationResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.concurrent.CompletableFuture;

/**
 * Controller for Forgot Password Scene
 * Handles password reset request
 */
public class ForgotPasswordScene {

    // ===== FXML Components =====
    @FXML private TextField emailField;
    @FXML private Button submitButton;
    @FXML private Hyperlink backToLoginLink;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Text titleText;
    @FXML private Text descriptionText;

    /**
     * Initialize controller - called after FXML is loaded
     */
    @FXML
    public void initialize() {
        setupEnterKeyHandlers();
        hideError();
    }

    /**
     * Setup Enter key to submit form
     */
    private void setupEnterKeyHandlers() {
        emailField.setOnAction(e -> handleSubmit());
    }

    // ===== Action Handlers =====

    /**
     * Handle submit button click - request password reset
     */
    @FXML
    private void handleSubmit() {
        String email = emailField.getText().trim();

        // Validation
        if (!validateInput(email)) {
            return;
        }

        // Show loading state
        setLoading(true);
        hideError();

        // Perform async password reset request
        CompletableFuture.runAsync(() -> {
            try {
                performPasswordResetRequest(email);
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
        SceneManager.setTitle("Discord Mini - Đăng nhập");
        SceneManager.loadContent("content/login-content.fxml");
    }

    // ===== Business Logic =====

    /**
     * Validate user input using ValidationUtil
     */
    private boolean validateInput(String email) {
        ValidationResult emailResult = ValidationUtil.validateEmail(email);
        if (!emailResult.isValid()) {
            showError(emailResult.getErrorMessage());
            return false;
        }
        
        return true;
    }

    /**
     * Perform password reset request using AuthService
     */
    private void performPasswordResetRequest(String email) {
        try {
            System.out.println("[ForgotPassword] Sending request for email: " + email);
            
            // Call the API through AuthService
            AuthService authService = AuthService.getInstance();
            ForgotPasswordResponse response = authService.forgotPassword(email);
            
            System.out.println("[ForgotPassword] Response received: " + response);
            
            // Handle response on UI thread
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    // Success - navigate to OTP verification
                    System.out.println("[ForgotPassword] Success! Navigating to OTP verification");
                    navigateToOTPVerification(email);
                } else {
                    // Server returned error message
                    System.out.println("[ForgotPassword] Error from server: " + response.getMessage());
                    showError(response.getMessage() != null ? response.getMessage() : "Không tìm thấy email. Vui lòng kiểm tra và thử lại.");
                }
            });
        } catch (java.net.SocketTimeoutException e) {
            // Specific handling for timeout
            System.err.println("[ForgotPassword] Timeout error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Yêu cầu mất quá nhiều thời gian. Vui lòng kiểm tra:\n1. Server đã chạy chưa?\n2. Kết nối mạng ổn định không?");
            });
        } catch (java.net.ConnectException e) {
            // Server not running
            System.err.println("[ForgotPassword] Connection error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Không thể kết nối đến server.\nVui lòng kiểm tra server đã chạy chưa.");
            });
        } catch (Exception e) {
            // Network or other error
            System.err.println("[ForgotPassword] General error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Lỗi kết nối: " + e.getMessage());
            });
        }
    }

    /**
     * Navigate to OTP verification scene
     */
    private void navigateToOTPVerification(String email) {
        // Swap content using SceneManager
        SceneManager.setTitle("Discord Mini - Xác minh OTP");
        OTPVerificationScene controller = (OTPVerificationScene) SceneManager.loadContent("content/otp-verification-content.fxml");
        if (controller != null) {
            controller.setEmail(email);
        }
    }

    /**
     * Handle errors
     */
    private void handleError(Exception e) {
        Platform.runLater(() -> {
            setLoading(false);
            showError("Lỗi kết nối: " + e.getMessage());
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
        emailField.setDisable(loading);
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
     * Hide error message
     */
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
