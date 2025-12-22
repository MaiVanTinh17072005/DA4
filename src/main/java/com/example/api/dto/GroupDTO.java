package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Group Data Transfer Object (Frontend)
 * Mirrors backend GroupDTO for API communication
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
    private String createdAt;
    
    // Constructors
    public GroupDTO() {
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
    
    @JsonProperty("memberIds")
    private java.util.List<Long> memberIds;
    
    public java.util.List<Long> getMemberIds() {
        return memberIds;
    }
    
    public void setMemberIds(java.util.List<Long> memberIds) {
        this.memberIds = memberIds;
    }
}
