package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Livestream entity model
 * Represents livestream sessions
 */
@Entity
@Table(name = "livestream")
public class Livestream {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stream_id")
    private Long streamId;
    
    @Column(name = "host_id", nullable = false)
    private Long hostId;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;
    
    @Column(length = 20, nullable = false)
    private String status = "active"; // active, ended
    
    // Constructors
    public Livestream() {
    }
    
    public Livestream(Long hostId, String title, String description) {
        this.hostId = hostId;
        this.title = title;
        this.description = description;
        this.startTime = LocalDateTime.now();
        this.viewCount = 0;
        this.status = "active";
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (viewCount == null) {
            viewCount = 0;
        }
        if (status == null) {
            status = "active";
        }
    }
    
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
        return "Livestream{" +
                "streamId=" + streamId +
                ", hostId=" + hostId +
                ", title='" + title + '\'' +
                ", startTime=" + startTime +
                ", viewCount=" + viewCount +
                ", status='" + status + '\'' +
                '}';
    }
}
