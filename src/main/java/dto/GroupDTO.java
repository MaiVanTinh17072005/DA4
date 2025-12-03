package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Group Data Transfer Object
 * Used for transferring group data between client and server
 */
public class GroupDTO {
    
    @JsonProperty("groupId")
    private Long groupId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("ownerId")
    private Long ownerId;
    
    @JsonProperty("ownerUsername")
    private String ownerUsername;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("memberCount")
    private Integer memberCount;
    
    @JsonProperty("createdAt")
    private String createdAt; // ISO format string
    
    // Constructors
    public GroupDTO() {
    }
    
    public GroupDTO(Long groupId, String name, Long ownerId, String ownerUsername, 
                   String description, Integer memberCount, String createdAt) {
        this.groupId = groupId;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.description = description;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
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
    
    public String getOwnerUsername() {
        return ownerUsername;
    }
    
    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
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
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "GroupDTO{" +
                "groupId=" + groupId +
                ", name='" + name + '\'' +
                ", ownerId=" + ownerId +
                ", ownerUsername='" + ownerUsername + '\'' +
                ", memberCount=" + memberCount +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
