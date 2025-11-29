package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Friend entity model
 * Represents friend relationships between users
 */
@Entity
@Table(name = "friend")
public class Friend {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_id")
    private Long friendId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    
    @Column(length = 20, nullable = false)
    private String status = "pending"; // pending, accepted, blocked
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public Friend() {
    }
    
    public Friend(Long userId, Long targetId, String status) {
        this.userId = userId;
        this.targetId = targetId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getFriendId() {
        return friendId;
    }
    
    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getTargetId() {
        return targetId;
    }
    
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Friend{" +
                "friendId=" + friendId +
                ", userId=" + userId +
                ", targetId=" + targetId +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
