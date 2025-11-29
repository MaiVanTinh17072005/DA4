package dto;

/**
 * Response DTO for password reset
 */
public class ResetPasswordResponse {
    private boolean success;
    private String message;

    public ResetPasswordResponse() {
    }

    public ResetPasswordResponse(boolean success, String message) {
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
    public static ResetPasswordResponse success(String message) {
        return new ResetPasswordResponse(true, message);
    }

    /**
     * Create error response
     */
    public static ResetPasswordResponse error(String message) {
        return new ResetPasswordResponse(false, message);
    }

    @Override
    public String toString() {
        return "ResetPasswordResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
