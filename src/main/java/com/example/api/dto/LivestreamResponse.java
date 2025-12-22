package com.example.api.dto;

/**
 * Response wrapper for livestream API calls
 */
public class LivestreamResponse {
    private boolean success;
    private String message;
    private LivestreamDTO livestream;
    
    public LivestreamResponse() {}
    
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
    
    public LivestreamDTO getLivestream() {
        return livestream;
    }
    
    public void setLivestream(LivestreamDTO livestream) {
        this.livestream = livestream;
    }
    
    @Override
    public String toString() {
        return "LivestreamResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", livestream=" + livestream +
                '}';
    }
}
