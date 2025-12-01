package dto;

/**
 * DTO for Accept/Reject Friend Request
 */
public class AcceptRequestDTO {
    
    private String requestId;
    
    public AcceptRequestDTO() {
    }
    
    public AcceptRequestDTO(String requestId) {
        this.requestId = requestId;
    }
    
    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    @Override
    public String toString() {
        return "AcceptRequestDTO{" +
                "requestId='" + requestId + '\'' +
                '}';
    }
}
