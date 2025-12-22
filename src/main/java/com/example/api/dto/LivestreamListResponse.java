package com.example.api.dto;

import java.util.List;

/**
 * Response for getting list of livestreams
 */
public class LivestreamListResponse {
    private boolean success;
    private String message;
    private List<LivestreamDTO> livestreams;
    
    public LivestreamListResponse() {}
    
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
    
    public List<LivestreamDTO> getLivestreams() {
        return livestreams;
    }
    
    public void setLivestreams(List<LivestreamDTO> livestreams) {
        this.livestreams = livestreams;
    }
    
    @Override
    public String toString() {
        return "LivestreamListResponse{" +
                "success=" + success +
                ", livestreamsCount=" + (livestreams != null ? livestreams.size() : 0) +
                '}';
    }
}
