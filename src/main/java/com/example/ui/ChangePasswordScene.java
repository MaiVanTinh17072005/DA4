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
 * Controller for Change Password Scene
 * Handles password reset after OTP verification
 */
public class ChangePasswordScene {

    // ===== FXML Components =====
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button submitButton;
    @FXML private Hyperlink backToLoginLink;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Text titleText;
    @FXML private Text descriptionText;

    // ===== State =====
    private String email;

    /**
     * Initialize controller - called after FXML is loaded
     */
    @FXML
    public void initialize() {
        setupEnterKeyHandlers();
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
        confirmPasswordField.setOnAction(e -> handleSubmit());
    }

    // ===== Action Handlers =====

    /**
     * Handle submit button click - change password
     */
    @FXML
    private void handleSubmit() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) newPasswordField.getScene().getWindow();
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
    private boolean validateInput(String newPassword, String confirmPassword) {
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return false;
        }

        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters");
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return false;
        }

        return true;
    }

    /**
     * Perform password change
     * TODO: Replace mock with actual BackendApi call
     */
    private void performPasswordChange(String newPassword) {
        // Mock API call delay
        simulateNetworkDelay(1500);

        // Mock success (replace with real API)
        boolean success = true;

        Platform.runLater(() -> {
            setLoading(false);
            if (success) {
                showSuccess("Password changed successfully! Redirecting to login...");
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
                showError("Failed to change password. Please try again.");
            }
        });

        // TODO: Actual implementation
        /*
        try {
            PasswordChangeResponse response = backendApi.changePassword(email, newPassword);
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    showSuccess("Password changed successfully! Redirecting to login...");
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            Platform.runLater(() -> handleBackToLogin());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
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
        confirmPasswordField.setDisable(loading);
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

