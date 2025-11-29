package controller;

import dto.AuthResponse;
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
     * Logout user (optional - mainly clears client-side token)
     * POST /api/v1/auth/logout
     * 
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout() {
        System.out.println("=== LOGOUT REQUEST ===");
        
        AuthResponse response = new AuthResponse(true, "Logout successful");
        
        System.out.println("======================");
        
        return ResponseEntity.ok(response);
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
