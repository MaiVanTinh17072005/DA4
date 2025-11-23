package com.example.util;

import java.util.regex.Pattern;

/**
 * Validation Utility Class
 * Provides validation methods for user inputs (email, password, username, etc.)
 */
public class ValidationUtil {

    // Email regex pattern: allows standard email format
    private static final String EMAIL_PATTERN = 
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    
    private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
    
    // Username pattern: 3-20 characters, alphanumeric and underscore only
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,20}$";
    
    private static final Pattern usernamePattern = Pattern.compile(USERNAME_PATTERN);
    
    // Password requirements
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 100;

    /**
     * Validate email format with specific error messages
     * @param email Email to validate
     * @return ValidationResult with isValid and error message
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(false, "Email không được để trống");
        }
        
        email = email.trim();
        
        // Check for @ symbol
        if (!email.contains("@")) {
            return new ValidationResult(false, "Email cần có ít nhất 1 ký tự @");
        }
        
        // Check if @ is at the beginning or end
        if (email.startsWith("@")) {
            return new ValidationResult(false, "Email không được bắt đầu bằng ký tự @");
        }
        
        if (email.endsWith("@")) {
            return new ValidationResult(false, "Email không được kết thúc bằng ký tự @");
        }
        
        // Check for multiple @ symbols
        int atCount = email.length() - email.replace("@", "").length();
        if (atCount > 1) {
            return new ValidationResult(false, "Email chỉ được có 1 ký tự @");
        }
        
        // Split by @ to check parts
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return new ValidationResult(false, "Email không hợp lệ");
        }
        
        String localPart = parts[0];
        String domainPart = parts[1];
        
        // Check local part (before @)
        if (localPart.isEmpty()) {
            return new ValidationResult(false, "Phần trước @ của email không được để trống");
        }
        
        // Check domain part (after @)
        if (domainPart.isEmpty()) {
            return new ValidationResult(false, "Phần sau @ của email không được để trống");
        }
        
        // Check for dot in domain
        if (!domainPart.contains(".")) {
            return new ValidationResult(false, "Phần sau @ cần có ít nhất 1 dấu chấm (.)");
        }
        
        // Check if domain starts or ends with dot
        if (domainPart.startsWith(".")) {
            return new ValidationResult(false, "Phần sau @ không được bắt đầu bằng dấu chấm");
        }
        
        if (domainPart.endsWith(".")) {
            return new ValidationResult(false, "Phần sau @ không được kết thúc bằng dấu chấm");
        }
        
        // Check TLD (top-level domain) - should be at least 2 characters after last dot
        String[] domainParts = domainPart.split("\\.");
        if (domainParts.length < 2) {
            return new ValidationResult(false, "Email cần có phần mở rộng (ví dụ: .com, .vn)");
        }
        
        String tld = domainParts[domainParts.length - 1];
        if (tld.length() < 2) {
            return new ValidationResult(false, "Phần mở rộng email phải có ít nhất 2 ký tự (ví dụ: .com, .vn)");
        }
        
        // Final pattern check
        if (!emailPattern.matcher(email).matches()) {
            return new ValidationResult(false, "Email không đúng định dạng (ví dụ: mai@gmail.com)");
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validate username format with specific error messages
     * @param username Username to validate
     * @return ValidationResult with isValid and error message
     */
    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new ValidationResult(false, "Tên người dùng không được để trống");
        }
        
        username = username.trim();
        
        // Check length - too short
        if (username.length() < 3) {
            return new ValidationResult(false, 
                String.format("Tên người dùng phải có ít nhất %d ký tự (hiện tại: %d ký tự)", 3, username.length()));
        }
        
        // Check length - too long
        if (username.length() > 20) {
            return new ValidationResult(false, 
                String.format("Tên người dùng không được vượt quá %d ký tự (hiện tại: %d ký tự)", 20, username.length()));
        }
        
        // Check for invalid characters
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            // Find first invalid character
            for (char c : username.toCharArray()) {
                if (!Character.isLetterOrDigit(c) && c != '_') {
                    return new ValidationResult(false, 
                        String.format("Tên người dùng không được chứa ký tự '%c'. Chỉ cho phép chữ cái, số và dấu gạch dưới (_)", c));
                }
            }
        }
        
        // Check if starts with number or underscore
        if (Character.isDigit(username.charAt(0))) {
            return new ValidationResult(false, "Tên người dùng không được bắt đầu bằng số");
        }
        
        if (username.startsWith("_")) {
            return new ValidationResult(false, "Tên người dùng không được bắt đầu bằng dấu gạch dưới (_)");
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validate password strength with specific error messages
     * Requirements:
     * - At least 6 characters
     * - Contains at least one uppercase letter
     * - Contains at least one lowercase letter
     * - Contains at least one digit
     * - Contains at least one special character
     * 
     * @param password Password to validate
     * @return ValidationResult with isValid and error message (only first error found)
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Mật khẩu không được để trống");
        }
        
        // Check length - too short (check first)
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new ValidationResult(false, 
                String.format("Mật khẩu phải có ít nhất %d ký tự (hiện tại: %d ký tự)", MIN_PASSWORD_LENGTH, password.length()));
        }
        
        // Check length - too long
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return new ValidationResult(false, 
                String.format("Mật khẩu không được vượt quá %d ký tự (hiện tại: %d ký tự)", MAX_PASSWORD_LENGTH, password.length()));
        }
        
        // Check for uppercase letter (check in order of importance)
        if (!password.matches(".*[A-Z].*")) {
            return new ValidationResult(false, 
                "Mật khẩu cần có ít nhất 1 chữ cái in hoa (A-Z)");
        }
        
        // Check for lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return new ValidationResult(false, 
                "Mật khẩu cần có ít nhất 1 chữ cái thường (a-z)");
        }
        
        // Check for digit
        if (!password.matches(".*[0-9].*")) {
            return new ValidationResult(false, 
                "Mật khẩu cần có ít nhất 1 chữ số (0-9)");
        }
        
        // Check for special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return new ValidationResult(false, 
                "Mật khẩu cần có ít nhất 1 ký tự đặc biệt (!@#$%^&*()_+-=[]{}|;':\",./<>?)");
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validate password for login (less strict - just check not empty)
     * @param password Password to validate
     * @return ValidationResult with isValid and error message
     */
    public static ValidationResult validatePasswordForLogin(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Mật khẩu không được để trống");
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validate that two passwords match
     * @param password First password
     * @param confirmPassword Confirmation password
     * @return ValidationResult with isValid and error message
     */
    public static ValidationResult validatePasswordMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return new ValidationResult(false, "Cả hai trường mật khẩu đều bắt buộc");
        }
        
        if (password.isEmpty() && confirmPassword.isEmpty()) {
            return new ValidationResult(false, "Mật khẩu không được để trống");
        }
        
        if (!password.equals(confirmPassword)) {
            return new ValidationResult(false, "Mật khẩu xác nhận không khớp với mật khẩu");
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validate email or username for login (can be either) with specific error messages
     * @param emailOrUsername Email or username to validate
     * @return ValidationResult with isValid and error message
     */
    public static ValidationResult validateEmailOrUsername(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            return new ValidationResult(false, "Email hoặc tên người dùng không được để trống");
        }
        
        String trimmed = emailOrUsername.trim();
        
        // Check if it's an email
        if (emailPattern.matcher(trimmed).matches()) {
            return new ValidationResult(true, null);
        }
        
        // Check if it contains @ - might be invalid email
        if (trimmed.contains("@")) {
            // Try to validate as email to get specific error
            ValidationResult emailResult = validateEmail(trimmed);
            if (!emailResult.isValid()) {
                return emailResult;
            }
        }
        
        // Check if it's a valid username
        ValidationResult usernameResult = validateUsername(trimmed);
        if (usernameResult.isValid()) {
            return new ValidationResult(true, null);
        }
        
        // If neither email nor username is valid, return username error (more specific)
        return usernameResult;
    }

    /**
     * Validation Result class
     * Contains validation status and error message
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

