package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Call History Data Transfer Object
 * Used for transferring call history data between client and server
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
    private String type; // voice, video, group
    
    @JsonProperty("startTime")
    private String startTime; // ISO format string
    
    @JsonProperty("endTime")
    private String endTime; // ISO format string
    
    @JsonProperty("duration")
    private Integer duration; // Duration in seconds
    
    @JsonProperty("success")
    private Boolean success;
    
    // Constructors
    public CallHistoryDTO() {
    }
    
    public CallHistoryDTO(Long callId, Long callerId, String callerUsername, 
                         Long receiverId, String receiverUsername, String type, 
                         String startTime, String endTime, Integer duration, Boolean success) {
        this.callId = callId;
        this.callerId = callerId;
        this.callerUsername = callerUsername;
        this.receiverId = receiverId;
        this.receiverUsername = receiverUsername;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.success = success;
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
    
    @Override
    public String toString() {
        return "CallHistoryDTO{" +
                "callId=" + callId +
                ", callerId=" + callerId +
                ", callerUsername='" + callerUsername + '\'' +
                ", receiverId=" + receiverId +
                ", receiverUsername='" + receiverUsername + '\'' +
                ", type='" + type + '\'' +
                ", startTime='" + startTime + '\'' +
                ", duration=" + duration +
                ", success=" + success +
                '}';
    }
}
