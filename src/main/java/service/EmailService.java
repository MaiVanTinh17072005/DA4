package service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email Service
 * Handles sending emails using JavaMailSender
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send OTP email to user
     * 
     * @param toEmail Recipient email address
     * @param otp 6-digit OTP code
     */
    public void sendOtpEmail(String toEmail, String otp) {
        System.out.println("[EmailService] Preparing to send OTP email...");
        System.out.println("[EmailService] To: " + toEmail);
        System.out.println("[EmailService] OTP: " + otp);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Discord Mini - Mã xác minh đặt lại mật khẩu");
            message.setText(buildOtpEmailContent(otp));
            
            System.out.println("[EmailService] Sending email...");
            mailSender.send(message);
            System.out.println("[EmailService] ✅ Email sent successfully!");
            
        } catch (Exception e) {
            System.err.println("[EmailService] ❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    /**
     * Build email content for OTP
     */
    private String buildOtpEmailContent(String otp) {
        return "Xin chào,\n\n" +
                "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Discord Mini của mình.\n\n" +
                "Mã xác minh của bạn là: " + otp + "\n\n" +
                "Mã này sẽ hết hạn sau 5 phút.\n\n" +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Discord Mini Team";
    }
}
