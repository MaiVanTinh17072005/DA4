package service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * OTP Service
 * Manages OTP generation and validation using in-memory storage
 */
@Service
public class OtpService {

    // In-memory storage for OTPs
    // Key: email, Value: OtpData (otp code and expiration time)
    private final Map<String, OtpData> otpStorage = new HashMap<>();

    // OTP expiration time in minutes
    private static final int OTP_EXPIRATION_MINUTES = 5;

    /**
     * Generate a random 6-digit OTP
     */
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Generate 6-digit number
        return String.valueOf(otp);
    }

    /**
     * Store OTP for email with expiration time
     * 
     * @param email User email
     * @param otp OTP code
     */
    public void storeOtp(String email, String otp) {
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);
        otpStorage.put(email, new OtpData(otp, expirationTime));
        
        System.out.println("[OtpService] OTP stored for email: " + email);
        System.out.println("[OtpService] OTP: " + otp);
        System.out.println("[OtpService] Expires at: " + expirationTime);
    }

    /**
     * Validate OTP for email
     * 
     * @param email User email
     * @param otp OTP code to validate
     * @return true if OTP is valid and not expired
     */
    public boolean validateOtp(String email, String otp) {
        OtpData otpData = otpStorage.get(email);
        
        if (otpData == null) {
            System.out.println("[OtpService] ❌ No OTP found for email: " + email);
            return false;
        }

        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(otpData.getExpirationTime())) {
            System.out.println("[OtpService] ❌ OTP expired for email: " + email);
            otpStorage.remove(email); // Remove expired OTP
            return false;
        }

        // Check if OTP matches
        if (!otpData.getOtp().equals(otp)) {
            System.out.println("[OtpService] ❌ Invalid OTP for email: " + email);
            return false;
        }

        System.out.println("[OtpService] ✅ OTP validated successfully for email: " + email);
        return true;
    }

    /**
     * Remove OTP after successful validation
     * 
     * @param email User email
     */
    public void removeOtp(String email) {
        otpStorage.remove(email);
        System.out.println("[OtpService] OTP removed for email: " + email);
    }

    /**
     * Inner class to store OTP data with expiration time
     */
    private static class OtpData {
        private final String otp;
        private final LocalDateTime expirationTime;

        public OtpData(String otp, LocalDateTime expirationTime) {
            this.otp = otp;
            this.expirationTime = expirationTime;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getExpirationTime() {
            return expirationTime;
        }
    }
}
