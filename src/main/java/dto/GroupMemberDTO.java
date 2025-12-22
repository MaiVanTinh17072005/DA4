package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Group Member Data Transfer Object
 * Used for transferring group member data between client and server
 */
public class GroupMemberDTO {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("groupId")
    private Long groupId;
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("avatarUrl")
    private String avatarUrl;
    
    @JsonProperty("role")
    private String role; // "member", "admin"
    
    @JsonProperty("joinedAt")
    private String joinedAt; // ISO format string
    
    // Constructors
    public GroupMemberDTO() {
    }
    
    public GroupMemberDTO(Long id, Long groupId, Long userId, String username, 
                         String avatarUrl, String role, String joinedAt) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.joinedAt = joinedAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(String joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    @Override
    public String toString() {
        return "GroupMemberDTO{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", joinedAt='" + joinedAt + '\'' +
                '}';
    }
}
