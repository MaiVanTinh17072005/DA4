package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * AIConfig entity model
 * Represents AI configuration settings for users
 */
@Entity
@Table(name = "ai_config")
public class AIConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "preferred_bitrate")
    private Integer preferredBitrate;
    
    @Column(length = 10)
    private String language;
    
    @Column(name = "theme_mode", length = 20)
    private String themeMode; // light, dark, auto
    
    @Column(name = "ai_enabled", nullable = false)
    private Boolean aiEnabled = true;
    
    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;
    
    // Constructors
    public AIConfig() {
    }
    
    public AIConfig(Long userId) {
        this.userId = userId;
        this.aiEnabled = true;
        this.lastUpdate = LocalDateTime.now();
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (lastUpdate == null) {
            lastUpdate = LocalDateTime.now();
        }
        if (aiEnabled == null) {
            aiEnabled = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Integer getPreferredBitrate() {
        return preferredBitrate;
    }
    
    public void setPreferredBitrate(Integer preferredBitrate) {
        this.preferredBitrate = preferredBitrate;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getThemeMode() {
        return themeMode;
    }
    
    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }
    
    public Boolean getAiEnabled() {
        return aiEnabled;
    }
    
    public void setAiEnabled(Boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }
    
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }
    
    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
    
    @Override
    public String toString() {
        return "AIConfig{" +
                "id=" + id +
                ", userId=" + userId +
                ", preferredBitrate=" + preferredBitrate +
                ", language='" + language + '\'' +
                ", themeMode='" + themeMode + '\'' +
                ", aiEnabled=" + aiEnabled +
                ", lastUpdate=" + lastUpdate +
                '}';
    }
}
