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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, 450, 650);
            scene.getStylesheets().add(getClass().getResource("/com/example/css/login.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Discord Mini - Login");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load login scene: " + e.getMessage());
        }
    }

    // ===== Business Logic =====

    /**
     * Validate user input
     */
    private boolean validateInput(String email) {
        if (email.isEmpty()) {
            showError("Please enter your email address");
            return false;
        }

        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email address");
            return false;
        }

        return true;
    }

    /**
     * Perform password reset request
     * TODO: Replace mock with actual BackendApi call
     */
    private void performPasswordResetRequest(String email) {
        // Mock API call delay
        simulateNetworkDelay(1500);

        // Mock success (replace with real API)
        boolean success = !email.equals("fail@test.com");

        Platform.runLater(() -> {
            setLoading(false);
            if (success) {
                navigateToOTPVerification(email);
            } else {
                showError("Email not found. Please check and try again.");
            }
        });

        // TODO: Actual implementation
        /*
        try {
            PasswordResetResponse response = backendApi.requestPasswordReset(email);
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    navigateToOTPVerification(email);
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
     * Navigate to OTP verification scene
     */
    private void navigateToOTPVerification(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/otp-verification.fxml"));
            Parent root = loader.load();

            // Pass email to OTP scene controller
            OTPVerificationScene controller = loader.getController();
            controller.setEmail(email);

            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, 450, 650);
            scene.getStylesheets().add(getClass().getResource("/com/example/css/otp-verification.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Discord Mini - Verify OTP");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load OTP verification: " + e.getMessage());
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

