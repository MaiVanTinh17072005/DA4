package util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility for password hashing and verification
 * Uses BCrypt for secure password storage
 */
public class PasswordUtil {
    
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * Hash a password using BCrypt
     * Note: The password received from client is already SHA-256 hashed
     * We hash it again with BCrypt for additional security
     * 
     * @param password SHA-256 hashed password from client
     * @return BCrypt hashed password for database storage
     */
    public static String hashPassword(String password) {
        return encoder.encode(password);
    }
    
    /**
     * Verify a password against a hashed password
     * 
     * @param rawPassword SHA-256 hashed password from client
     * @param hashedPassword BCrypt hashed password from database
     * @return true if passwords match
     */
    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
