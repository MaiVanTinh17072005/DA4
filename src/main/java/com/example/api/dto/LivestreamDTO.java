package com.example.api.dto;

import java.time.LocalDateTime;

/**
 * DTO for livestream information
 */
public class LivestreamDTO {
    private Long streamId;
    private Long hostId;
    private String hostName;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer viewCount;
    private String status;
    
    public LivestreamDTO() {}
    
    // Getters and Setters
    public Long getStreamId() {
        return streamId;
    }
    
    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }
    
    public Long getHostId() {
        return hostId;
    }
    
    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }
    
    public String getHostName() {
        return hostName;
    }
    
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public Integer getViewCount() {
        return viewCount;
    }
    
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "LivestreamDTO{" +
                "streamId=" + streamId +
                ", hostName='" + hostName + '\'' +
                ", title='" + title + '\'' +
                ", viewCount=" + viewCount +
                ", status='" + status + '\'' +
                '}';
    }
}
