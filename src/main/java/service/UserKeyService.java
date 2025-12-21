package service;

import model.UserKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.UserKeyRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing user public keys for E2EE
 */
@Service
public class UserKeyService {
    
    @Autowired
    private UserKeyRepository userKeyRepository;
    
    /**
     * Save or update user's public key
     * 
     * @param userId ID of the user
     * @param publicKey Base64 encoded ECDH public key
     */
    public void savePublicKey(Long userId, String publicKey) {
        Optional<UserKey> existingKey = userKeyRepository.findById(userId);
        
        UserKey userKey;
        if (existingKey.isPresent()) {
            userKey = existingKey.get();
            userKey.setPublicKey(publicKey);
            userKey.setUpdatedAt(LocalDateTime.now());
        } else {
            userKey = new UserKey(userId, publicKey);
        }
        
        userKeyRepository.save(userKey);
        System.out.println("[UserKeyService] Saved public key for user: " + userId);
    }
    
    /**
     * Get user's public key
     * 
     * @param userId ID of the user
     * @return Base64 encoded ECDH public key, or null if not found
     */
    public String getPublicKey(Long userId) {
        return userKeyRepository.findById(userId)
                .map(UserKey::getPublicKey)
                .orElse(null);
    }
}
