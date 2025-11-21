package com.example.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for Login/Register Scene
 * Handles user authentication and navigation to main application
 */
public class LoginScene {

    // ===== FXML Components =====
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button actionButton;
    @FXML private Hyperlink toggleLink;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Text welcomeText;
    @FXML private Text togglePromptText;
    @FXML private VBox confirmPasswordBox;
    @FXML private HBox forgotPasswordBox;

    // ===== State =====
    private boolean isLoginMode = true;

    // ===== Services (TODO: Inject later) =====
    // private AuthService authService;
    // private BackendApi backendApi;

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
        passwordField.setOnAction(e -> handleAction());
        confirmPasswordField.setOnAction(e -> handleAction());
    }

    // ===== Action Handlers =====

    /**
     * Handle Login or Register button click
     */
    @FXML
    private void handleAction() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (!validateInput(email, password)) {
            return;
        }

        // Show loading state
        setLoading(true);
        hideError();

        // Perform async authentication
        CompletableFuture.runAsync(() -> {
            try {
                if (isLoginMode) {
                    performLogin(email, password);
                } else {
                    performRegister(email, password);
                }
            } catch (Exception e) {
                handleAuthError(e);
            }
        });
    }

    /**
     * Toggle between Login and Register mode
     */
    @FXML
    private void handleToggleMode() {
        isLoginMode = !isLoginMode;
        updateUIForMode();
        clearFields();
        hideError();
    }

    /**
     * Handle forgot password link - navigate to forgot password scene
     */
    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/forgot-password.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
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

    // ===== Authentication Logic =====

    /**
     * Validate user input
     */
    private boolean validateInput(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return false;
        }

        if (!isLoginMode) {
            String confirmPass = confirmPasswordField.getText();
            if (!password.equals(confirmPass)) {
                showError("Passwords do not match");
                return false;
            }
            if (password.length() < 6) {
                showError("Password must be at least 6 characters");
                return false;
            }
        }

        return true;
    }

    /**
     * Perform login authentication
     * TODO: Replace mock with actual BackendApi call
     */
    private void performLogin(String email, String password) {
        // Mock API call delay
        simulateNetworkDelay(1500);

        // Mock authentication (replace with real API)
        boolean success = !email.equals("fail@test.com");

        Platform.runLater(() -> {
            setLoading(false);
            if (success) {
                navigateToMainScene();
            } else {
                showError("Invalid credentials. Please try again.");
            }
        });

        // TODO: Actual implementation
        /*
        try {
            AuthResponse response = backendApi.login(email, password);
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    // Save session
                    SessionManager.setCurrentUser(response.getUser());
                    navigateToMainScene();
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
     * Perform user registration
     * TODO: Replace mock with actual BackendApi call
     */
    private void performRegister(String email, String password) {
        simulateNetworkDelay(1500);

        boolean success = true; // Mock success

        Platform.runLater(() -> {
            setLoading(false);
            if (success) {
                showSuccess("Account created successfully! Please log in.");
                switchToLoginMode();
            } else {
                showError("Registration failed. Email may already exist.");
            }
        });

        // TODO: Actual implementation
        /*
        try {
            AuthResponse response = backendApi.register(email, password);
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    showSuccess("Account created! Please log in.");
                    switchToLoginMode();
                } else {
                    showError(response.getErrorMessage());
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                setLoading(false);
                showError("Registration error: " + e.getMessage());
            });
        }
        */
    }

    /**
     * Handle authentication errors
     */
    private void handleAuthError(Exception e) {
        Platform.runLater(() -> {
            setLoading(false);
            showError("Connection error: " + e.getMessage());
        });
    }

    // ===== Navigation =====

    /**
     * Navigate to main application scene after successful login
     */
    private void navigateToMainScene() {
        try {
            // Load ChatScene (main view)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Parent root = loader.load();

            // Get current stage
            Stage stage = (Stage) emailField.getScene().getWindow();

            // Create new scene
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/chat.css").toExternalForm());

            // Set scene and show
            stage.setScene(scene);
            stage.setTitle("Discord Mini - Chat");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load main scene: " + e.getMessage());
        }
    }

    // ===== UI State Management =====

    /**
     * Update UI elements based on current mode (Login/Register)
     */
    private void updateUIForMode() {
        if (isLoginMode) {
            switchToLoginMode();
        } else {
            switchToRegisterMode();
        }
    }

    /**
     * Switch UI to Login mode
     */
    private void switchToLoginMode() {
        isLoginMode = true;
        welcomeText.setText("Welcome back!");
        actionButton.setText("Log In");
        togglePromptText.setText("Need an account?");
        toggleLink.setText("Register");

        confirmPasswordBox.setVisible(false);
        confirmPasswordBox.setManaged(false);
        forgotPasswordBox.setVisible(true);
        forgotPasswordBox.setManaged(true);
    }

    /**
     * Switch UI to Register mode
     */
    private void switchToRegisterMode() {
        isLoginMode = false;
        welcomeText.setText("Create an account");
        actionButton.setText("Register");
        togglePromptText.setText("Already have an account?");
        toggleLink.setText("Log In");

        confirmPasswordBox.setVisible(true);
        confirmPasswordBox.setManaged(true);
        forgotPasswordBox.setVisible(false);
        forgotPasswordBox.setManaged(false);
    }

    /**
     * Set loading state (disable inputs, show spinner)
     */
    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
        actionButton.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        toggleLink.setDisable(loading);
    }

    /**
     * Clear all input fields
     */
    private void clearFields() {
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
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

    /**
     * Show information dialog
     */
    private void showInfoDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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