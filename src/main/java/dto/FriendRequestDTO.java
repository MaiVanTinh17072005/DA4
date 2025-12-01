package dto;

/**
 * DTO for Friend Request
 * Used when sending friend request
 */
public class FriendRequestDTO {
    
    private Long targetId;
    
    public FriendRequestDTO() {
    }
    
    public FriendRequestDTO(Long targetId) {
        this.targetId = targetId;
    }
    
    // Getters and Setters
    public Long getTargetId() {
        return targetId;
    }
    
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
    
    @Override
    public String toString() {
        return "FriendRequestDTO{" +
                "targetId=" + targetId +
                '}';
    }
}
