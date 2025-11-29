package dto;

/**
 * Response DTO for OTP verification
 */
public class VerifyOtpResponse {
    private boolean success;
    private String message;

    public VerifyOtpResponse() {
    }

    public VerifyOtpResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Create success response
     */
    public static VerifyOtpResponse success(String message) {
        return new VerifyOtpResponse(true, message);
    }

    /**
     * Create error response
     */
    public static VerifyOtpResponse error(String message) {
        return new VerifyOtpResponse(false, message);
    }

    @Override
    public String toString() {
        return "VerifyOtpResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
