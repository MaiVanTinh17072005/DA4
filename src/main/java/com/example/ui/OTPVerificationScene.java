package com.example.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
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
    @FXML private VBox otpContainer;

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/forgot-password.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) otpField1.getScene().getWindow();
            Scene scene = new Scene(root, 450, 650);
            scene.getStylesheets().add(getClass().getResource("/com/example/css/forgot-password.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Discord Mini - Forgot Password");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load forgot password scene: " + e.getMessage());
        }
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
     * Perform OTP verification
     * TODO: Replace mock with actual BackendApi call
     */
    private void performOTPVerification(String otp) {
        // Mock API call delay
        simulateNetworkDelay(1500);

        // Mock verification (replace with real API)
        // Accept any OTP except "000000" for testing
        boolean success = !otp.equals("000000");

        Platform.runLater(() -> {
            setLoading(false);
            if (success) {
                navigateToChangePassword();
            } else {
                showError("Invalid verification code. Please try again.");
                clearOTPFields();
            }
        });

        // TODO: Actual implementation
        /*
        try {
            OTPVerificationResponse response = backendApi.verifyOTP(email, otp);
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    navigateToChangePassword();
                } else {
                    showError(response.getErrorMessage());
                    clearOTPFields();
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                setLoading(false);
                showError("Connection error: " + e.getMessage());
            });
        }
        */
    }

    /**
     * Resend OTP code
     * TODO: Replace mock with actual BackendApi call
     */
    private void resendOTP() {
        simulateNetworkDelay(1000);

        boolean success = true; // Mock success

        Platform.runLater(() -> {
            setLoading(false);
            if (success) {
                showSuccess("Verification code has been resent to your email.");
                clearOTPFields();
                startResendTimer();
            } else {
                showError("Failed to resend code. Please try again.");
            }
        });

        // TODO: Actual implementation
        /*
        try {
            PasswordResetResponse response = backendApi.requestPasswordReset(email);
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    showSuccess("Verification code has been resent.");
                    clearOTPFields();
                    startResendTimer();
                } else {
                    showError(response.getErrorMessage());
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                setLoading(false);
                showError("Connection error: " + e.getMessage());
            });
        }
        */
    }

    /**
     * Navigate to change password scene
     */
    private void navigateToChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/change-password.fxml"));
            Parent root = loader.load();

            // Pass email to change password controller
            ChangePasswordScene controller = loader.getController();
            controller.setEmail(email);

            Stage stage = (Stage) otpField1.getScene().getWindow();
            Scene scene = new Scene(root, 450, 700);
            scene.getStylesheets().add(getClass().getResource("/com/example/css/change-password.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Discord Mini - Change Password");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load change password scene: " + e.getMessage());
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

