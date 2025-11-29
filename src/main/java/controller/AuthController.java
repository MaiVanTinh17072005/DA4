package controller;

import dto.AuthResponse;
import dto.ForgotPasswordRequest;
import dto.ForgotPasswordResponse;
import dto.LoginRequest;
import dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.AuthService;

/**
 * Authentication Controller
 * REST API endpoints for user authentication
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Allow requests from JavaFX client
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    /**
     * Register new user
     * POST /api/v1/auth/register
     * 
     * @param request RegisterRequest with email, username, password (SHA-256 hashed)
     * @return AuthResponse with success status, message, token, and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        System.out.println("=== REGISTER REQUEST ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("Username: " + request.getUsername());
        System.out.println("Password: [HASHED]");
        
        AuthResponse response = authService.register(request);
        
        System.out.println("Response: " + response);
        System.out.println("========================");
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Login user
     * POST /api/v1/auth/login
     * 
     * @param request LoginRequest with email and password (SHA-256 hashed)
     * @return AuthResponse with success status, message, token, and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        System.out.println("=== LOGIN REQUEST ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("Password: [HASHED]");
        
        AuthResponse response = authService.login(request);
        
        System.out.println("Response: " + response);
        System.out.println("=====================");
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    /**
     * Logout user
     * POST /api/v1/auth/logout
     * Updates user status to offline
     * 
     * @param authHeader Authorization header containing JWT token
     * @return AuthResponse with success status and message
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        System.out.println("=== LOGOUT REQUEST ===");
        System.out.println("Authorization Header: " + (authHeader != null ? "Present" : "Missing"));
        
        // Validate Authorization header
        if (authHeader == null || authHeader.trim().isEmpty()) {
            System.out.println("❌ Missing Authorization header");
            AuthResponse response = AuthResponse.error("Authorization header is required");
            System.out.println("======================");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        // Call service to handle logout
        AuthResponse response = authService.logout(authHeader);
        
        System.out.println("Response: " + response);
        System.out.println("======================");
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Forgot password - Send OTP to user's email
     * POST /api/v1/auth/forgot-password
     * 
     * @param request ForgotPasswordRequest with email
     * @return ForgotPasswordResponse with success status and message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        System.out.println("=== FORGOT PASSWORD REQUEST ===");
        System.out.println("Email: " + request.getEmail());
        
        ForgotPasswordResponse response = authService.forgotPassword(request);
        
        System.out.println("Response: " + response);
        System.out.println("===============================");
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Health check endpoint
     * GET /api/v1/auth/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}
