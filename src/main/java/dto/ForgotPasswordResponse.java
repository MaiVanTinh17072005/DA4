package dto;

/**
 * Response DTO for forgot password
 */
public class ForgotPasswordResponse {
    private boolean success;
    private String message;

    public ForgotPasswordResponse() {
    }

    public ForgotPasswordResponse(boolean success, String message) {
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
    public static ForgotPasswordResponse success(String message) {
        return new ForgotPasswordResponse(true, message);
    }

    /**
     * Create error response
     */
    public static ForgotPasswordResponse error(String message) {
        return new ForgotPasswordResponse(false, message);
    }

    @Override
    public String toString() {
        return "ForgotPasswordResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
