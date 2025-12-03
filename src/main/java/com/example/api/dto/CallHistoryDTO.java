package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Call History Data Transfer Object (Frontend)
 * Mirrors backend CallHistoryDTO for API communication
 */
public class CallHistoryDTO {
    
    @JsonProperty("callId")
    private Long callId;
    
    @JsonProperty("callerId")
    private Long callerId;
    
    @JsonProperty("callerUsername")
    private String callerUsername;
    
    @JsonProperty("receiverId")
    private Long receiverId;
    
    @JsonProperty("receiverUsername")
    private String receiverUsername;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("startTime")
    private String startTime;
    
    @JsonProperty("endTime")
    private String endTime;
    
    @JsonProperty("duration")
    private Integer duration;
    
    @JsonProperty("success")
    private Boolean success;
    
    // Constructors
    public CallHistoryDTO() {
    }
    
    // Getters and Setters
    public Long getCallId() {
        return callId;
    }
    
    public void setCallId(Long callId) {
        this.callId = callId;
    }
    
    public Long getCallerId() {
        return callerId;
    }
    
    public void setCallerId(Long callerId) {
        this.callerId = callerId;
    }
    
    public String getCallerUsername() {
        return callerUsername;
    }
    
    public void setCallerUsername(String callerUsername) {
        this.callerUsername = callerUsername;
    }
    
    public Long getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getReceiverUsername() {
        return receiverUsername;
    }
    
    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }
    
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
