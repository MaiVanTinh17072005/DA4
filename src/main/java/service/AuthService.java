package service;

import dto.AuthResponse;
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
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    /**
     * Register a new user
     * 
     * @param request Registration request with email, username, and SHA-256 hashed password
     * @return AuthResponse with success status, message, token, and user info
     */
    public AuthResponse register(RegisterRequest request) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return AuthResponse.error("Email is required");
            }
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return AuthResponse.error("Username is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return AuthResponse.error("Password is required");
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return AuthResponse.error("Email already exists");
            }
            
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                return AuthResponse.error("Username already exists");
            }
            
            // Hash the password (client already sent SHA-256, we hash again with BCrypt)
            String hashedPassword = PasswordUtil.hashPassword(request.getPassword());
            
            // Create new user
            User user = new User(
                    request.getEmail(),
                    request.getUsername(),
                    hashedPassword
            );
            
            // Save user to database
            user = userRepository.save(user);
            
            // Generate JWT token
            String token = JwtUtil.generateToken(user.getId(), user.getEmail(), user.getUsername());
            
            // Convert to DTO
            UserDTO userDTO = convertToDTO(user);
            
            // Return success response
            return AuthResponse.success("Registration successful", token, userDTO);
            
        } catch (Exception e) {
            e.printStackTrace();
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
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return AuthResponse.error("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return AuthResponse.error("Password is required");
            }
            
            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            
            if (userOptional.isEmpty()) {
                return AuthResponse.error("Invalid credentials");
            }
            
            User user = userOptional.get();
            
            // Verify password (compare SHA-256 from client with BCrypt in database)
            if (!PasswordUtil.verifyPassword(request.getPassword(), user.getPassword())) {
                return AuthResponse.error("Invalid credentials");
            }
            
            // Update user status to online
            user.setStatus("online");
            userRepository.save(user);
            
            // Generate JWT token
            String token = JwtUtil.generateToken(user.getId(), user.getEmail(), user.getUsername());
            
            // Convert to DTO
            UserDTO userDTO = convertToDTO(user);
            
            // Return success response
            return AuthResponse.success("Login successful", token, userDTO);
            
        } catch (Exception e) {
            e.printStackTrace();
            return AuthResponse.error("Login failed: " + e.getMessage());
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
