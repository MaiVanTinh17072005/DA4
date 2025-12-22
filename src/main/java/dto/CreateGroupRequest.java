package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for creating a new group
 */
public class CreateGroupRequest {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("memberIds")
    private List<Long> memberIds; // Initial members to add (excluding owner)
    
    // Constructors
    public CreateGroupRequest() {
    }
    
    public CreateGroupRequest(String name, String description, List<Long> memberIds) {
        this.name = name;
        this.description = description;
        this.memberIds = memberIds;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<Long> getMemberIds() {
        return memberIds;
    }
    
    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds;
    }
    
    @Override
    public String toString() {
        return "CreateGroupRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", memberIds=" + memberIds +
                '}';
    }
}
