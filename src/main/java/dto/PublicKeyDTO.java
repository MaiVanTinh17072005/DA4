package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for transferring public keys
 */
public class PublicKeyDTO {
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("publicKey")
    private String publicKey;
    
    public PublicKeyDTO() {
    }
    
    public PublicKeyDTO(Long userId, String publicKey) {
        this.userId = userId;
        this.publicKey = publicKey;
    }
    
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
}
