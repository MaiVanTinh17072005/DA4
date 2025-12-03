package com.example.ui;

import com.example.api.dto.AuthResponse;
import com.example.service.AuthService;
import com.example.util.SceneManager;
import com.example.util.SessionManager;
import com.example.util.ValidationUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

import com.example.network.PortAllocator;
import com.example.network.PeerManager;

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
            ValidationUtil.ValidationResult emailResult = ValidationUtil.validateEmail(email);
            if (!emailResult.isValid()) {
                showError(emailResult.getErrorMessage());
                return false;
            }
            
            ValidationUtil.ValidationResult passwordResult = ValidationUtil.validatePasswordForLogin(password);
            if (!passwordResult.isValid()) {
                showError(passwordResult.getErrorMessage());
                return false;
            }
        } else {
            // Register mode: email, username, password, and confirm password required
            // Email validation
            ValidationUtil.ValidationResult emailResult = ValidationUtil.validateEmail(email);
            if (!emailResult.isValid()) {
                showError(emailResult.getErrorMessage());
                return false;
            }
            
            // Username validation
            ValidationUtil.ValidationResult usernameResult = ValidationUtil.validateUsername(username);
            if (!usernameResult.isValid()) {
                showError(usernameResult.getErrorMessage());
                return false;
            }
            
            // Password validation
            ValidationUtil.ValidationResult passwordResult = ValidationUtil.validatePassword(password);
            if (!passwordResult.isValid()) {
                showError(passwordResult.getErrorMessage());
                return false;
            }
            
            // Confirm password validation
            String confirmPass = isConfirmPasswordVisible && confirmPasswordTextField != null
                ? confirmPasswordTextField.getText()
                : confirmPasswordField.getText();
            
            ValidationUtil.ValidationResult passwordMatchResult = ValidationUtil.validatePasswordMatch(password, confirmPass);
            if (!passwordMatchResult.isValid()) {
                showError(passwordMatchResult.getErrorMessage());
                return false;
            }
        }

        return true;
    }

    /**
     * Perform login authentication
     * Sends login request to server via AuthService
     */
    private void performLogin(String email, String password) {
        try {
            // Call AuthService to login (password will be hashed automatically)
            AuthResponse response = AuthService.getInstance().login(email, password);
            
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    // Save session
                    SessionManager.setCurrentUser(response.getUser());
                    SessionManager.setAuthToken(response.getToken());
                    
                    // Initialize P2P for this user
                    initializeP2PForUser();
                    
                    // Show success message
                    System.out.println("Login successful! User: " + response.getUser().getUsername());
                    
                    // Navigate to main scene
                    navigateToMainScene();
                } else {
                    showError(response.getMessage() != null ? response.getMessage() : "Đăng nhập thất bại");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Lỗi kết nối: " + e.getMessage());
            });
        }
    }

    /**
     * Perform user registration
     * Sends registration request to server via AuthService
     */
    private void performRegister(String email, String username, String password) {
        try {
            // Call AuthService to register (password will be hashed automatically)
            AuthResponse response = AuthService.getInstance().register(email, username, password);
            
            Platform.runLater(() -> {
                setLoading(false);
                if (response.isSuccess()) {
                    // Show success message
                    showSuccess("Tài khoản đã được tạo thành công! Vui lòng đăng nhập.");
                    System.out.println("Registration successful! User: " + response.getUser().getUsername());
                    
                    // Switch to login mode
                    switchToLoginMode();
                } else {
                    showError(response.getMessage() != null ? response.getMessage() : "Đăng ký thất bại");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                setLoading(false);
                showError("Lỗi đăng ký: " + e.getMessage());
            });
        }
    }

    /**
     * Initialize P2P for current user
     * Allocates TCP/UDP ports and starts PeerManager
     */
    private void initializeP2PForUser() {
        try {
            // Allocate ports
            int tcpPort = PortAllocator.allocatePort();
            int udpPort = PortAllocator.allocatePort();
            
            if (tcpPort == -1 || udpPort == -1) {
                System.err.println("❌ [P2P] Failed to allocate ports");
                return;
            }
            
            // Start P2P
            // PeerManager.getInstance().startP2P(tcpPort, udpPort); // DISABLED: Port conflict with P2PConnectionManager
            
            // Save to session
            SessionManager.setP2PPorts(tcpPort, udpPort);
            
            // Get local IP
            String localIP = InetAddress.getLocalHost().getHostAddress();
            
            System.out.println("✅ [P2P] Initialized successfully");
            System.out.println("   └─ Local IP: " + localIP);
            System.out.println("   └─ TCP Port: " + tcpPort);
            System.out.println("   └─ UDP Port: " + udpPort);
            
            // Send P2P info to server
            sendP2PInfoToServer(localIP, tcpPort, udpPort);
            
        } catch (Exception e) {
            System.err.println("❌ [P2P] Initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send P2P information to server
     * Runs asynchronously to avoid blocking UI
     */
    private void sendP2PInfoToServer(String ipAddress, int tcpPort, int udpPort) {
        CompletableFuture.runAsync(() -> {
            try {
                Long userId = SessionManager.getCurrentUserId();
                if (userId == null) {
                    System.err.println("⚠ [P2P] Cannot send info - user ID is null");
                    return;
                }
                
                System.out.println("📡 [P2P] Sending info to server...");
                
                com.example.service.P2PService p2pService = com.example.service.P2PService.getInstance();
                com.example.api.dto.P2PInfoResponse response = p2pService.registerP2PInfo(
                    userId, ipAddress, tcpPort, udpPort
                );
                
                if (response.isSuccess()) {
                    System.out.println("✅ [P2P] Info sent to server successfully");
                } else {
                    System.err.println("❌ [P2P] Failed to send info: " + response.getMessage());
                }
                
            } catch (Exception e) {
                System.err.println("❌ [P2P] Error sending info to server: " + e.getMessage());
                e.printStackTrace();
                // Non-critical error - P2P still works locally
            }
        });
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
            SceneManager.loadContent("content/chat-content.fxml");
            
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