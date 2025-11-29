package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Group entity model
 * Represents chat groups
 */
@Entity
@Table(name = "groups")
public class Group {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "member_count", nullable = false)
    private Integer memberCount = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public Group() {
    }
    
    public Group(String name, Long ownerId, String description) {
        this.name = name;
        this.ownerId = ownerId;
        this.description = description;
        this.memberCount = 1; // Owner is the first member
        this.createdAt = LocalDateTime.now();
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (memberCount == null) {
            memberCount = 0;
        }
    }
    
    // Getters and Setters
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Long getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getMemberCount() {
        return memberCount;
    }
    
    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Group{" +
                "groupId=" + groupId +
                ", name='" + name + '\'' +
                ", ownerId=" + ownerId +
                ", memberCount=" + memberCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
