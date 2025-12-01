package controller;

import dto.P2PInfoRequest;
import dto.P2PInfoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.P2PService;

/**
 * P2P Controller
 * REST API endpoints for P2P connection management
 */
@RestController
@RequestMapping("/api/v1/p2p")
@CrossOrigin(origins = "*") // Allow requests from JavaFX client
public class P2PController {
    
    @Autowired
    private P2PService p2pService;
    
    /**
     * Register or update P2P information
     * POST /api/v1/p2p/register
     * 
     * @param request P2PInfoRequest with userId, ipAddress, tcpPort, udpPort
     * @return P2PInfoResponse with success status and message
     */
    @PostMapping("/register")
    public ResponseEntity<P2PInfoResponse> registerP2PInfo(@RequestBody P2PInfoRequest request) {
        System.out.println("=== P2P REGISTER REQUEST ===");
        System.out.println("User ID: " + request.getUserId());
        System.out.println("IP Address: " + request.getIpAddress());
        System.out.println("TCP Port: " + request.getTcpPort());
        System.out.println("UDP Port: " + request.getUdpPort());
        
        // Validate request
        if (request.getUserId() == null) {
            System.out.println("❌ Missing user ID");
            P2PInfoResponse response = P2PInfoResponse.error("User ID is required");
            System.out.println("============================");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        if (request.getIpAddress() == null || request.getIpAddress().trim().isEmpty()) {
            System.out.println("❌ Missing IP address");
            P2PInfoResponse response = P2PInfoResponse.error("IP address is required");
            System.out.println("============================");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        if (request.getTcpPort() == null || request.getUdpPort() == null) {
            System.out.println("❌ Missing ports");
            P2PInfoResponse response = P2PInfoResponse.error("TCP and UDP ports are required");
            System.out.println("============================");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Register P2P info
        boolean success = p2pService.registerP2PInfo(request);
        
        P2PInfoResponse response;
        if (success) {
            response = P2PInfoResponse.success("P2P info registered successfully");
            System.out.println("✅ P2P info registered");
            System.out.println("============================");
            return ResponseEntity.ok(response);
        } else {
            response = P2PInfoResponse.error("Failed to register P2P info");
            System.out.println("❌ Failed to register P2P info");
            System.out.println("============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get P2P information for a peer
     * GET /api/v1/p2p/peer/{userId}
     * 
     * @param userId User ID of the peer
     * @return P2PInfoRequest with peer's connection info
     */
    @GetMapping("/peer/{userId}")
    public ResponseEntity<P2PInfoRequest> getPeerInfo(@PathVariable Long userId) {
        System.out.println("=== P2P GET PEER INFO REQUEST ===");
        System.out.println("Requested User ID: " + userId);
        
        // Get P2P info from Redis
        P2PInfoRequest p2pInfo = p2pService.getP2PInfo(userId);
        
        if (p2pInfo != null) {
            System.out.println("✅ Peer info found");
            System.out.println("=================================");
            return ResponseEntity.ok(p2pInfo);
        } else {
            System.out.println("❌ Peer not found or offline");
            System.out.println("=================================");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    
    /**
     * Remove P2P information (called on logout)
     * DELETE /api/v1/p2p/user/{userId}
     * 
     * @param userId User ID
     * @return P2PInfoResponse with success status
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<P2PInfoResponse> removeP2PInfo(@PathVariable Long userId) {
        System.out.println("=== P2P REMOVE REQUEST ===");
        System.out.println("User ID: " + userId);
        
        boolean success = p2pService.removeP2PInfo(userId);
        
        P2PInfoResponse response;
        if (success) {
            response = P2PInfoResponse.success("P2P info removed successfully");
            System.out.println("✅ P2P info removed");
            System.out.println("==========================");
            return ResponseEntity.ok(response);
        } else {
            response = P2PInfoResponse.error("P2P info not found");
            System.out.println("⚠ P2P info not found");
            System.out.println("==========================");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * Health check endpoint
     * GET /api/v1/p2p/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("P2P service is running");
    }
}
