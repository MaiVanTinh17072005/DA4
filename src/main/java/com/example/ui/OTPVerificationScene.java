package com.example.ui;

import com.example.api.dto.ForgotPasswordResponse;
import com.example.api.dto.VerifyOtpResponse;
import com.example.service.AuthService;
import com.example.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.concurrent.CompletableFuture;

/**
 * Controller for OTP Verification Scene
 * Handles OTP code verification for password reset
 */
public class OTPVerificationScene {

    // ===== FXML Components =====
    @FXML private TextField otpField1;
    @FXML private TextField otpField2;
    @FXML private TextField otpField3;
    @FXML private TextField otpField4;
    @FXML private TextField otpField5;
    @FXML private TextField otpField6;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;
    @FXML private Hyperlink backLink;
    @FXML private Label errorLabel;
    @FXML private Label timerLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Text titleText;
    @FXML private Text descriptionText;
    @FXML private HBox otpContainer;

    // ===== State =====
    private String email;
    private int resendTimer = 60; // 60 seconds countdown

    /**
     * Initialize controller - called after FXML is loaded
     */
    @FXML
    public void initialize() {
        setupOTPFields();
        setupEnterKeyHandlers();
        hideError();
        startResendTimer();
    }

    /**
     * Set email for this session
     */
    public void setEmail(String email) {
        this.email = email;
        // Update description text if already initialized
        Platform.runLater(() -> {
            if (descriptionText != null && email != null) {
                descriptionText.setText("We've sent a verification code to " + email);
            }
        });
    }

    /**
     * Setup OTP input fields with auto-focus and navigation
     */
    private void setupOTPFields() {
        TextField[] fields = {otpField1, otpField2, otpField3, otpField4, otpField5, otpField6};

        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            TextField field = fields[i];

            // Limit to single digit
            field.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.length() > 1) {
                    field.setText(newValue.substring(0, 1));
                }
                // Only allow digits
                if (!newValue.matches("\\d*")) {
                    field.setText(newValue.replaceAll("[^\\d]", ""));
                }
                // Auto-focus next field
                if (newValue.length() == 1 && index < fields.length - 1) {
                    fields[index + 1].requestFocus();
                }
            });

            // Handle backspace to go to previous field
            field.setOnKeyPressed(e -> {
                if (e.getCode().toString().equals("BACK_SPACE") && field.getText().isEmpty() && index > 0) {
                    fields[index - 1].requestFocus();
                }
            });
        }
    }

    /**
     * Setup Enter key to verify OTP
     */
    private void setupEnterKeyHandlers() {
        otpField6.setOnAction(e -> handleVerify());
    }

    // ===== Action Handlers =====

    /**
     * Handle verify button click
     */
    @FXML
    private void handleVerify() {
        String otp = getOTPCode();

        // Validation
        if (!validateOTP(otp)) {
            return;
        }

        // Show loading state
        setLoading(true);
        hideError();

        // Perform async OTP verification
        CompletableFuture.runAsync(() -> {
            try {
                performOTPVerification(otp);
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    /**
     * Handle resend OTP button click
     */
    @FXML
    private void handleResend() {
        if (resendTimer > 0) {
            return; // Still in countdown
        }

        setLoading(true);
        hideError();

        CompletableFuture.runAsync(() -> {
            try {
                resendOTP();
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    /**
     * Navigate back to forgot password scene
     */
    @FXML
    private void handleBack() {
        // Swap content using SceneManager
        SceneManager.setTitle("Discord Mini - Forgot Password");
        SceneManager.loadContent("content/forgot-password-content.fxml");
    }

    // ===== Business Logic =====

    /**
     * Get OTP code from all fields
     */
    private String getOTPCode() {
        return otpField1.getText() +
               otpField2.getText() +
               otpField3.getText() +
               otpField4.getText() +
               otpField5.getText() +
               otpField6.getText();
    }

    /**
     * Validate OTP input
     */
    private boolean validateOTP(String otp) {
        if (otp.length() != 6) {
            showError("Please enter the complete 6-digit code");
            return false;
        }

        if (!otp.matches("\\d{6}")) {
            showError("OTP code must contain only digits");
            return false;
        }

        return true;
    }

    /**
     * Perform OTP verification using AuthService
     */
    private void performOTPVerification(String otp) {
        try {
            System.out.println("[OTPVerification] Verifying OTP for email: " + email);
            
            // Call the API through AuthService
            AuthService authService = AuthService.getInstance();
            VerifyOtpResponse response = authService.verifyOtp(email, otp);
            
            System.out.println("[OTPVerification] Response received: " + response);
            
            // Handle response on UI thread
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    // Success - navigate to change password
                    System.out.println("[OTPVerification] Success! Navigating to change password");
                    navigateToChangePassword();
                } else {
                    // Server returned error message
                    System.out.println("[OTPVerification] Error from server: " + response.getMessage());
                    showError(response.getMessage() != null ? response.getMessage() : "Mã OTP không hợp lệ. Vui lòng thử lại.");
                    clearOTPFields();
                }
            });
        } catch (java.net.SocketTimeoutException e) {
            // Timeout error
            System.err.println("[OTPVerification] Timeout error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Yêu cầu mất quá nhiều thời gian. Vui lòng thử lại.");
            });
        } catch (java.net.ConnectException e) {
            // Server not running
            System.err.println("[OTPVerification] Connection error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Không thể kết nối đến server.\nVui lòng kiểm tra server đã chạy chưa.");
            });
        } catch (Exception e) {
            // Network or other error
            System.err.println("[OTPVerification] General error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Lỗi kết nối: " + e.getMessage());
            });
        }
    }

    /**
     * Resend OTP code using AuthService
     */
    private void resendOTP() {
        try {
            System.out.println("[OTPVerification] Resending OTP for email: " + email);
            
            // Call the forgot password API again to resend OTP
            AuthService authService = AuthService.getInstance();
            ForgotPasswordResponse response = authService.forgotPassword(email);
            
            System.out.println("[OTPVerification] Resend response: " + response);
            
            // Handle response on UI thread
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    System.out.println("[OTPVerification] OTP resent successfully");
                    showSuccess("Mã xác minh đã được gửi lại đến email của bạn.");
                    clearOTPFields();
                    startResendTimer();
                } else {
                    System.out.println("[OTPVerification] Resend failed: " + response.getMessage());
                    showError(response.getMessage() != null ? response.getMessage() : "Không thể gửi lại mã. Vui lòng thử lại.");
                }
            });
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[OTPVerification] Timeout error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Yêu cầu mất quá nhiều thời gian. Vui lòng thử lại.");
            });
        } catch (java.net.ConnectException e) {
            System.err.println("[OTPVerification] Connection error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Không thể kết nối đến server.");
            });
        } catch (Exception e) {
            System.err.println("[OTPVerification] General error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Lỗi kết nối: " + e.getMessage());
            });
        }
    }

    /**
     * Navigate to change password scene
     */
    private void navigateToChangePassword() {
        // Swap content using SceneManager
        SceneManager.setTitle("Discord Mini - Change Password");
        ChangePasswordScene controller = (ChangePasswordScene) SceneManager.loadContent("content/change-password-content.fxml");
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
            showError("Connection error: " + e.getMessage());
        });
    }

    // ===== UI State Management =====

    /**
     * Set loading state
     */
    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
        verifyButton.setDisable(loading);
        resendButton.setDisable(loading || resendTimer > 0);
        otpField1.setDisable(loading);
        otpField2.setDisable(loading);
        otpField3.setDisable(loading);
        otpField4.setDisable(loading);
        otpField5.setDisable(loading);
        otpField6.setDisable(loading);
    }

    /**
     * Clear all OTP fields
     */
    private void clearOTPFields() {
        otpField1.clear();
        otpField2.clear();
        otpField3.clear();
        otpField4.clear();
        otpField5.clear();
        otpField6.clear();
        otpField1.requestFocus();
    }

    /**
     * Start resend timer countdown
     */
    private void startResendTimer() {
        resendTimer = 60;
        updateResendButton();

        // Countdown timer
        new Thread(() -> {
            while (resendTimer > 0) {
                try {
                    Thread.sleep(1000);
                    resendTimer--;
                    Platform.runLater(() -> updateResendButton());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    /**
     * Update resend button text based on timer
     */
    private void updateResendButton() {
        if (resendTimer > 0) {
            resendButton.setText("Resend Code (" + resendTimer + "s)");
            resendButton.setDisable(true);
            if (timerLabel != null) {
                timerLabel.setText("Resend code in " + resendTimer + " seconds");
                timerLabel.setVisible(true);
            }
        } else {
            resendButton.setText("Resend Code");
            resendButton.setDisable(false);
            if (timerLabel != null) {
                timerLabel.setVisible(false);
            }
        }
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

