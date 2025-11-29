package service;

import dto.AuthResponse;
import dto.ForgotPasswordRequest;
import dto.ForgotPasswordResponse;
import dto.LoginRequest;
import dto.RegisterRequest;
import dto.UserDTO;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.UserRepository;
import util.JwtUtil;
import util.PasswordUtil;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Authentication Service
 * Handles user registration and login business logic
 */
@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private OtpService otpService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    /**
     * Register a new user
     * 
     * @param request Registration request with email, username, and SHA-256 hashed password
     * @return AuthResponse with success status, message, token, and user info
     */
    public AuthResponse register(RegisterRequest request) {
        System.out.println("[AuthService] ===== REGISTER PROCESS START =====");
        try {
            // Validate input
            System.out.println("[AuthService] Step 1: Validating input...");
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                System.out.println("[AuthService] ❌ Validation failed: Email is empty");
                return AuthResponse.error("Email is required");
            }
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                System.out.println("[AuthService] ❌ Validation failed: Username is empty");
                return AuthResponse.error("Username is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                System.out.println("[AuthService] ❌ Validation failed: Password is empty");
                return AuthResponse.error("Password is required");
            }
            System.out.println("[AuthService] ✓ Input validation passed");
            
            // Check if email already exists
            System.out.println("[AuthService] Step 2: Checking if email exists in database...");
            if (userRepository.existsByEmail(request.getEmail())) {
                System.out.println("[AuthService] ❌ Email already exists: " + request.getEmail());
                return AuthResponse.error("Email already exists");
            }
            System.out.println("[AuthService] ✓ Email is available");
            
            // Check if username already exists
            System.out.println("[AuthService] Step 3: Checking if username exists in database...");
            if (userRepository.existsByUsername(request.getUsername())) {
                System.out.println("[AuthService] ❌ Username already exists: " + request.getUsername());
                return AuthResponse.error("Username already exists");
            }
            System.out.println("[AuthService] ✓ Username is available");
            
            // Hash the password (client already sent SHA-256, we hash again with BCrypt)
            System.out.println("[AuthService] Step 4: Hashing password with BCrypt...");
            String hashedPassword = PasswordUtil.hashPassword(request.getPassword());
            System.out.println("[AuthService] ✓ Password hashed successfully");
            
            // Create new user
            System.out.println("[AuthService] Step 5: Creating new user object...");
            User user = new User(
                    request.getEmail(),
                    request.getUsername(),
                    hashedPassword
            );
            System.out.println("[AuthService] ✓ User object created");
            
            // Save user to database
            System.out.println("[AuthService] Step 6: Saving user to database...");
            user = userRepository.save(user);
            System.out.println("[AuthService] ✓ User saved to database with ID: " + user.getId());
            
            // Generate JWT token
            System.out.println("[AuthService] Step 7: Generating JWT token...");
            String token = JwtUtil.generateToken(user.getId(), user.getEmail(), user.getUsername());
            System.out.println("[AuthService] ✓ JWT token generated");
            
            // Convert to DTO
            System.out.println("[AuthService] Step 8: Converting to DTO...");
            UserDTO userDTO = convertToDTO(user);
            System.out.println("[AuthService] ✓ DTO created");
            
            // Return success response
            System.out.println("[AuthService] ✅ Registration successful for user: " + user.getUsername());
            System.out.println("[AuthService] ===== REGISTER PROCESS END =====");
            return AuthResponse.success("Registration successful", token, userDTO);
            
        } catch (Exception e) {
            System.out.println("[AuthService] ❌❌❌ EXCEPTION OCCURRED ❌❌❌");
            System.out.println("[AuthService] Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("[AuthService] ===== REGISTER PROCESS END (FAILED) =====");
            return AuthResponse.error("Registration failed: " + e.getMessage());
        }
    }
    
    /**
     * Login user
     * 
     * @param request Login request with email and SHA-256 hashed password
     * @return AuthResponse with success status, message, token, and user info
     */
    public AuthResponse login(LoginRequest request) {
        System.out.println("[AuthService] ===== LOGIN PROCESS START =====");
        try {
            // Validate input
            System.out.println("[AuthService] Step 1: Validating input...");
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                System.out.println("[AuthService] ❌ Validation failed: Email is empty");
                return AuthResponse.error("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                System.out.println("[AuthService] ❌ Validation failed: Password is empty");
                return AuthResponse.error("Password is required");
            }
            System.out.println("[AuthService] ✓ Input validation passed");
            
            // Find user by email
            System.out.println("[AuthService] Step 2: Finding user in database by email: " + request.getEmail());
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            
            if (userOptional.isEmpty()) {
                System.out.println("[AuthService] ❌ User not found with email: " + request.getEmail());
                return AuthResponse.error("Invalid credentials");
            }
            System.out.println("[AuthService] ✓ User found in database");
            
            User user = userOptional.get();
            System.out.println("[AuthService] User ID: " + user.getId() + ", Username: " + user.getUsername());
            
            // Verify password (compare SHA-256 from client with BCrypt in database)
            System.out.println("[AuthService] Step 3: Verifying password...");
            if (!PasswordUtil.verifyPassword(request.getPassword(), user.getPassword())) {
                System.out.println("[AuthService] ❌ Password verification failed for user: " + user.getUsername());
                return AuthResponse.error("Invalid credentials");
            }
            System.out.println("[AuthService] ✓ Password verified successfully");
            
            // Update user status to online
            System.out.println("[AuthService] Step 4: Updating user status to 'online'...");
            user.setStatus("online");
            userRepository.save(user);
            System.out.println("[AuthService] ✓ User status updated to 'online'");
            
            // Generate JWT token
            System.out.println("[AuthService] Step 5: Generating JWT token...");
            String token = JwtUtil.generateToken(user.getId(), user.getEmail(), user.getUsername());
            System.out.println("[AuthService] ✓ JWT token generated");
            
            // Convert to DTO
            System.out.println("[AuthService] Step 6: Converting to DTO...");
            UserDTO userDTO = convertToDTO(user);
            System.out.println("[AuthService] ✓ DTO created");
            
            // Return success response
            System.out.println("[AuthService] ✅ Login successful for user: " + user.getUsername() + " (ID: " + user.getId() + ")");
            System.out.println("[AuthService] ===== LOGIN PROCESS END =====");
            return AuthResponse.success("Login successful", token, userDTO);
            
        } catch (Exception e) {
            System.out.println("[AuthService] ❌❌❌ EXCEPTION OCCURRED ❌❌❌");
            System.out.println("[AuthService] Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("[AuthService] ===== LOGIN PROCESS END (FAILED) =====");
            return AuthResponse.error("Login failed: " + e.getMessage());
        }
    }
    
    /**
     * Logout user
     * Updates user status to offline
     * 
     * @param token JWT token from Authorization header
     * @return AuthResponse with success status and message
     */
    public AuthResponse logout(String token) {
        System.out.println("[AuthService] ===== LOGOUT PROCESS START =====");
        try {
            // Validate token
            System.out.println("[AuthService] Step 1: Validating JWT token...");
            if (token == null || token.trim().isEmpty()) {
                System.out.println("[AuthService] ❌ Token is empty");
                return AuthResponse.error("Token is required");
            }
            
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Validate token format
            if (!JwtUtil.validateToken(token)) {
                System.out.println("[AuthService] ❌ Invalid token");
                return AuthResponse.error("Invalid token");
            }
            System.out.println("[AuthService] ✓ Token is valid");
            
            // Extract user ID from token
            System.out.println("[AuthService] Step 2: Extracting user ID from token...");
            Long userId = JwtUtil.extractUserId(token);
            System.out.println("[AuthService] ✓ User ID extracted: " + userId);
            
            // Find user in database
            System.out.println("[AuthService] Step 3: Finding user in database...");
            Optional<User> userOptional = userRepository.findById(userId);
            
            if (userOptional.isEmpty()) {
                System.out.println("[AuthService] ❌ User not found with ID: " + userId);
                return AuthResponse.error("User not found");
            }
            System.out.println("[AuthService] ✓ User found in database");
            
            User user = userOptional.get();
            System.out.println("[AuthService] User: " + user.getUsername() + " (ID: " + user.getId() + ")");
            
            // Update user status to offline
            System.out.println("[AuthService] Step 4: Updating user status to 'offline'...");
            String previousStatus = user.getStatus();
            user.setStatus("offline");
            userRepository.save(user);
            System.out.println("[AuthService] ✓ User status updated from '" + previousStatus + "' to 'offline'");
            
            // Return success response
            System.out.println("[AuthService] ✅ Logout successful for user: " + user.getUsername() + " (ID: " + user.getId() + ")");
            System.out.println("[AuthService] ===== LOGOUT PROCESS END =====");
            return AuthResponse.success("Logout successful", null, null);
            
        } catch (Exception e) {
            System.out.println("[AuthService] ❌❌❌ EXCEPTION OCCURRED ❌❌❌");
            System.out.println("[AuthService] Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("[AuthService] ===== LOGOUT PROCESS END (FAILED) =====");
            return AuthResponse.error("Logout failed: " + e.getMessage());
        }
    }
    
    /**
     * Forgot password - Send OTP to user's email
     * 
     * @param request ForgotPasswordRequest with email
     * @return ForgotPasswordResponse with success status and message
     */
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        System.out.println("[AuthService] ===== FORGOT PASSWORD PROCESS START =====");
        try {
            // Validate input
            System.out.println("[AuthService] Step 1: Validating input...");
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                System.out.println("[AuthService] ❌ Validation failed: Email is empty");
                return ForgotPasswordResponse.error("Email is required");
            }
            System.out.println("[AuthService] ✓ Input validation passed");
            
            // Check if email exists in database
            System.out.println("[AuthService] Step 2: Checking if email exists...");
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            
            if (userOptional.isEmpty()) {
                System.out.println("[AuthService] ❌ Email not found: " + request.getEmail());
                return ForgotPasswordResponse.error("Email không tồn tại trong hệ thống");
            }
            System.out.println("[AuthService] ✓ Email found in database");
            
            User user = userOptional.get();
            System.out.println("[AuthService] User: " + user.getUsername() + " (ID: " + user.getId() + ")");
            
            // Generate 6-digit OTP
            System.out.println("[AuthService] Step 3: Generating OTP...");
            String otp = otpService.generateOtp();
            System.out.println("[AuthService] ✓ OTP generated: " + otp);
            
            // Store OTP with expiration time
            System.out.println("[AuthService] Step 4: Storing OTP...");
            otpService.storeOtp(request.getEmail(), otp);
            System.out.println("[AuthService] ✓ OTP stored successfully");
            
            // Send OTP via email
            System.out.println("[AuthService] Step 5: Sending OTP email...");
            emailService.sendOtpEmail(request.getEmail(), otp);
            System.out.println("[AuthService] ✓ OTP email sent successfully");
            
            // Return success response
            System.out.println("[AuthService] ✅ Forgot password process successful for email: " + request.getEmail());
            System.out.println("[AuthService] ===== FORGOT PASSWORD PROCESS END =====");
            return ForgotPasswordResponse.success("Mã OTP đã được gửi đến email của bạn");
            
        } catch (Exception e) {
            System.out.println("[AuthService] ❌❌❌ EXCEPTION OCCURRED ❌❌❌");
            System.out.println("[AuthService] Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("[AuthService] ===== FORGOT PASSWORD PROCESS END (FAILED) =====");
            return ForgotPasswordResponse.error("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }
    
    /**
     * Convert User entity to UserDTO
     * 
     * @param user User entity
     * @return UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt().format(DATE_FORMATTER));
        return dto;
    }
}
