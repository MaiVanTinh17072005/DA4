package controller;

import dto.PublicKeyDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.UserKeyService;
import util.JwtUtil;

import java.util.Map;

/**
 * Controller for managing user public keys for E2EE
 */
@RestController
@RequestMapping("/api/v1/keys")
public class UserKeyController {
    
    @Autowired
    private UserKeyService userKeyService;
    
    /**
     * Upload user's public key
     * 
     * @param publicKeyDTO DTO containing public key
     * @param authorization Bearer token
     * @return Success response
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPublicKey(
            @RequestBody PublicKeyDTO publicKeyDTO,
            @RequestHeader("Authorization") String authorization
    ) {
        System.out.println("[UserKeyController] Upload request received");
        
        try {
            Long userId = JwtUtil.extractUserIdFromHeader(authorization);
            userKeyService.savePublicKey(userId, publicKeyDTO.getPublicKey());
            return ResponseEntity.ok(Map.of("success", true, "message", "Public key uploaded successfully"));
        } catch (Exception e) {
            System.err.println("[UserKeyController] Error uploading public key: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Get friend's public key
     * 
     * @param userId ID of the friend
     * @return DTO containing public key
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPublicKey(@PathVariable Long userId) {
        System.out.println("[UserKeyController] Get request received for user: " + userId);
        
        String publicKey = userKeyService.getPublicKey(userId);
        if (publicKey != null) {
            return ResponseEntity.ok(new PublicKeyDTO(userId, publicKey));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
