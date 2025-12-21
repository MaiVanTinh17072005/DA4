package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * UserKey entity model
 * Stores user's ECDH public key for E2EE
 */
@Entity
@Table(name = "user_key")
public class UserKey {
    
    @Id
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "public_key", columnDefinition = "TEXT", nullable = false)
    private String publicKey; // Base64 encoded ECDH public key
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public UserKey() {
    }
    
    public UserKey(Long userId, String publicKey) {
        this.userId = userId;
        this.publicKey = publicKey;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
