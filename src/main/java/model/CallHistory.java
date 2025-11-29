package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * CallHistory entity model
 * Represents call history records
 */
@Entity
@Table(name = "call_history")
public class CallHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "call_id")
    private Long callId;
    
    @Column(name = "caller_id", nullable = false)
    private Long callerId;
    
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;
    
    @Column(length = 20, nullable = false)
    private String type; // voice, video, group
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column
    private Integer duration; // Duration in seconds
    
    @Column(name = "bitrate_ai_suggest")
    private Integer bitrateAiSuggest;
    
    @Column(nullable = false)
    private Boolean success = false;
    
    // Constructors
    public CallHistory() {
    }
    
    public CallHistory(Long callerId, Long receiverId, String type) {
        this.callerId = callerId;
        this.receiverId = receiverId;
        this.type = type;
        this.startTime = LocalDateTime.now();
        this.success = false;
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (success == null) {
            success = false;
        }
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
    
    public Long getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Integer getBitrateAiSuggest() {
        return bitrateAiSuggest;
    }
    
    public void setBitrateAiSuggest(Integer bitrateAiSuggest) {
        this.bitrateAiSuggest = bitrateAiSuggest;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    @Override
    public String toString() {
        return "CallHistory{" +
                "callId=" + callId +
                ", callerId=" + callerId +
                ", receiverId=" + receiverId +
                ", type='" + type + '\'' +
                ", startTime=" + startTime +
                ", duration=" + duration +
                ", success=" + success +
                '}';
    }
}
