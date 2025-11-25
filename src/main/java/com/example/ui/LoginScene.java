package com.example.ui;

import com.example.util.SceneManager;
import com.example.util.ValidationUtil;
import com.example.util.ValidationUtil.ValidationResult;
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
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField; // For showing password
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField; // For showing password
    @FXML private Button actionButton;
    @FXML private Button passwordToggleButton;
    @FXML private Button confirmPasswordToggleButton;
    @FXML private Hyperlink toggleLink;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Label errorLabel;
    @FXML private Label emailLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Text welcomeText;
    @FXML private Text togglePromptText;
    @FXML private VBox confirmPasswordBox;
    @FXML private VBox usernameBox;
    @FXML private HBox forgotPasswordBox;
    
    // ===== Password Visibility State =====
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

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
        setupPasswordVisibilitySync();
        hideError();
    }
    
    /**
     * Setup Enter key to submit form
     */
    private void setupEnterKeyHandlers() {
        passwordField.setOnAction(e -> handleAction());
        if (passwordTextField != null) {
            passwordTextField.setOnAction(e -> handleAction());
        }
        confirmPasswordField.setOnAction(e -> handleAction());
        if (confirmPasswordTextField != null) {
            confirmPasswordTextField.setOnAction(e -> handleAction());
        }
        if (usernameField != null) {
            usernameField.setOnAction(e -> handleAction());
        }
    }
    
    /**
     * Sync password between PasswordField and TextField
     */
    private void setupPasswordVisibilitySync() {
        if (passwordTextField != null && passwordField != null) {
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isPasswordVisible && passwordTextField != null) {
                    passwordTextField.setText(newVal);
                }
            });
            
            passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isPasswordVisible && passwordField != null) {
                    passwordField.setText(newVal);
                }
            });
        }
        
        if (confirmPasswordTextField != null && confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isConfirmPasswordVisible && confirmPasswordTextField != null) {
                    confirmPasswordTextField.setText(newVal);
                }
            });
            
            confirmPasswordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isConfirmPasswordVisible && confirmPasswordField != null) {
                    confirmPasswordField.setText(newVal);
                }
            });
        }
    }
    
    /**
     * Toggle password visibility
     */
    @FXML
    private void togglePasswordVisibility() {
        if (passwordField == null || passwordTextField == null) return;
        
        isPasswordVisible = !isPasswordVisible;
        
        if (isPasswordVisible) {
            passwordTextField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordToggleButton.setText("🙈");
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordToggleButton.setText("👁");
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
     * Handle Login or Register button click
     */
    @FXML
    private void handleAction() {
        String email = emailField.getText().trim();
        String username = usernameField != null ? usernameField.getText().trim() : "";
        String password = isPasswordVisible && passwordTextField != null 
            ? passwordTextField.getText() 
            : passwordField.getText();

        // Validation
        if (!validateInput(email, username, password)) {
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
                    performRegister(email, username, password);
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
        // Swap content using SceneManager (no scene reload needed)
        SceneManager.setTitle("Discord Mini - Quên Mật Khẩu");
        SceneManager.loadContent("content/forgot-password-content.fxml");
    }

    // ===== Authentication Logic =====

    /**
     * Validate user input using ValidationUtil
     */
    private boolean validateInput(String email, String username, String password) {
        if (isLoginMode) {
            // Login mode: email and password required (email only, no username)
            ValidationResult emailResult = ValidationUtil.validateEmail(email);
            if (!emailResult.isValid()) {
                showError(emailResult.getErrorMessage());
                return false;
            }
            
            ValidationResult passwordResult = ValidationUtil.validatePasswordForLogin(password);
            if (!passwordResult.isValid()) {
                showError(passwordResult.getErrorMessage());
                return false;
            }
        } else {
            // Register mode: email, username, password, and confirm password required
            // Email validation
            ValidationResult emailResult = ValidationUtil.validateEmail(email);
            if (!emailResult.isValid()) {
                showError(emailResult.getErrorMessage());
                return false;
            }
            
            // Username validation
            ValidationResult usernameResult = ValidationUtil.validateUsername(username);
            if (!usernameResult.isValid()) {
                showError(usernameResult.getErrorMessage());
                return false;
            }
            
            // Password validation
            ValidationResult passwordResult = ValidationUtil.validatePassword(password);
            if (!passwordResult.isValid()) {
                showError(passwordResult.getErrorMessage());
                return false;
            }
            
            // Confirm password validation
            String confirmPass = isConfirmPasswordVisible && confirmPasswordTextField != null
                ? confirmPasswordTextField.getText()
                : confirmPasswordField.getText();
            
            ValidationResult passwordMatchResult = ValidationUtil.validatePasswordMatch(password, confirmPass);
            if (!passwordMatchResult.isValid()) {
                showError(passwordMatchResult.getErrorMessage());
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
            //test chức năng login vào trang chủ
            navigateToMainScene();
//            if (success) {
//                navigateToMainScene();
//            } else {
//                showError("Invalid credentials. Please try again.");
//            }
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
    private void performRegister(String email, String username, String password) {
        simulateNetworkDelay(1500);

        boolean success = true; // Mock success

        Platform.runLater(() -> {
            setLoading(false);
            if (success) {
                showSuccess("Tài khoản đã được tạo thành công! Vui lòng đăng nhập.");
                switchToLoginMode();
            } else {
                showError("Đăng ký thất bại. Email hoặc tên người dùng có thể đã tồn tại.");
            }
        });

        // TODO: Actual implementation
        /*
        try {
            AuthResponse response = backendApi.register(email, username, password);
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
            showError("Lỗi kết nối: " + e.getMessage());
        });
    }

    /**
     * Navigate to main application scene after successful login
     */
    private void navigateToMainScene() {
        try {
            // Use SceneManager to load chat content
            // This ensures proper alignment and layout handling
            SceneManager.setTitle("Discord Mini - Chat");
            SceneManager.loadContent("chat.fxml");
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Không thể tải giao diện chính: " + e.getMessage());
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
        welcomeText.setText("Chào mừng trở lại!");
        actionButton.setText("Đăng nhập");
        togglePromptText.setText("Chưa có tài khoản?");
        toggleLink.setText("Đăng ký");
        if (emailLabel != null) {
            emailLabel.setText("EMAIL");
        }
        if (emailField != null) {
            emailField.setPromptText("Nhập địa chỉ email của bạn");
        }

        usernameBox.setVisible(false);
        usernameBox.setManaged(false);
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
        welcomeText.setText("Tạo tài khoản mới");
        actionButton.setText("Đăng ký");
        togglePromptText.setText("Đã có tài khoản?");
        toggleLink.setText("Đăng nhập");
        if (emailLabel != null) {
            emailLabel.setText("EMAIL");
        }

        usernameBox.setVisible(true);
        usernameBox.setManaged(true);
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
        if (usernameField != null) {
            usernameField.setDisable(loading);
        }
        passwordField.setDisable(loading);
        if (passwordTextField != null) {
            passwordTextField.setDisable(loading);
        }
        confirmPasswordField.setDisable(loading);
        if (confirmPasswordTextField != null) {
            confirmPasswordTextField.setDisable(loading);
        }
        if (passwordToggleButton != null) {
            passwordToggleButton.setDisable(loading);
        }
        if (confirmPasswordToggleButton != null) {
            confirmPasswordToggleButton.setDisable(loading);
        }
        toggleLink.setDisable(loading);
    }

    /**
     * Clear all input fields
     */
    private void clearFields() {
        emailField.clear();
        if (usernameField != null) {
            usernameField.clear();
        }
        passwordField.clear();
        if (passwordTextField != null) {
            passwordTextField.clear();
        }
        confirmPasswordField.clear();
        if (confirmPasswordTextField != null) {
            confirmPasswordTextField.clear();
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