package controller;

import dto.FriendRequestDTO;
import dto.FriendResponseDTO;
import dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.FriendService;
import util.JwtUtil;

import java.util.List;

/**
 * Friend Controller
 * REST API endpoints for friend management
 */
@RestController
@RequestMapping("/api/v1/friends")
@CrossOrigin(origins = "*") // Allow requests from JavaFX client
public class FriendController {
    
    @Autowired
    private FriendService friendService;
    
    /**
     * Get friend suggestions
     * GET /api/v1/friends/suggestions?limit=10
     * 
     * @param limit Maximum number of suggestions (default 10)
     * @param authorization JWT token from Authorization header
     * @return List of suggested users
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<UserDTO>> getFriendSuggestions(
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== GET FRIEND SUGGESTIONS ===");
        System.out.println("Limit: " + limit);
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Get suggestions
            List<UserDTO> suggestions = friendService.getFriendSuggestions(userId, limit);
            
            System.out.println("✅ Returning " + suggestions.size() + " suggestions");
            System.out.println("==============================");
            return ResponseEntity.ok(suggestions);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("==============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Search users
     * GET /api/v1/friends/search?query=john
     * 
     * @param query Search query (username or email)
     * @param authorization JWT token from Authorization header
     * @return List of matching users
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(
            @RequestParam(name = "query") String query,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== SEARCH USERS ===");
        System.out.println("Query: " + query);
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Search users
            List<UserDTO> results = friendService.searchUsers(userId, query);
            
            System.out.println("✅ Found " + results.size() + " users");
            System.out.println("====================");
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("====================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Send friend request
     * POST /api/v1/friends/request
     * Body: { "targetId": 123 }
     * 
     * @param request Friend request DTO
     * @param authorization JWT token from Authorization header
     * @return Success/error response
     */
    @PostMapping("/request")
    public ResponseEntity<FriendResponseDTO> sendFriendRequest(
            @RequestBody FriendRequestDTO request,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== SEND FRIEND REQUEST ===");
        System.out.println("Target ID: " + request.getTargetId());
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("Sender ID: " + userId);
            
            // Validate
            if (request.getTargetId() == null) {
                System.err.println("❌ Target ID is required");
                System.out.println("===========================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(FriendResponseDTO.error("Target ID is required"));
            }
            
            // Send request
            boolean success = friendService.sendFriendRequest(userId, request.getTargetId());
            
            if (success) {
                System.out.println("✅ Friend request sent");
                System.out.println("===========================");
                return ResponseEntity.ok(FriendResponseDTO.success("Friend request sent successfully"));
            } else {
                System.err.println("❌ Failed to send friend request");
                System.out.println("===========================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(FriendResponseDTO.error("Failed to send friend request"));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("===========================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FriendResponseDTO.error("Internal server error"));
        }
    }
    
    /**
     * Cancel friend request
     * DELETE /api/v1/friends/cancel/{targetId}
     * 
     * @param targetId Target user ID
     * @param authorization JWT token from Authorization header
     * @return Success/error response
     */
    @DeleteMapping("/cancel/{targetId}")
    public ResponseEntity<FriendResponseDTO> cancelFriendRequest(
            @PathVariable("targetId") Long targetId,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== CANCEL FRIEND REQUEST ===");
        System.out.println("Target ID: " + targetId);
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Cancel request
            boolean success = friendService.cancelFriendRequest(userId, targetId);
            
            if (success) {
                System.out.println("✅ Friend request cancelled");
                System.out.println("=============================");
                return ResponseEntity.ok(FriendResponseDTO.success("Friend request cancelled successfully"));
            } else {
                System.err.println("❌ Failed to cancel friend request");
                System.out.println("=============================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(FriendResponseDTO.error("Failed to cancel friend request"));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FriendResponseDTO.error("Internal server error"));
        }
    }
    
    /**
     * Get pending friend requests
     * GET /api/v1/friends/pending
     * 
     * @param authorization JWT token from Authorization header
     * @return List of pending friend requests
     */
    @GetMapping("/pending")
    public ResponseEntity<List<dto.PendingFriendRequestDTO>> getPendingRequests(
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== GET PENDING REQUESTS ===");
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Get pending requests
            List<dto.PendingFriendRequestDTO> requests = friendService.getPendingRequests(userId);
            
            System.out.println("✅ Returning " + requests.size() + " pending requests");
            System.out.println("============================");
            return ResponseEntity.ok(requests);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Accept friend request
     * POST /api/v1/friends/accept
     * Body: { "requestId": "req_456_123" }
     * 
     * @param request Accept request DTO
     * @param authorization JWT token from Authorization header
     * @return Success/error response
     */
    @PostMapping("/accept")
    public ResponseEntity<FriendResponseDTO> acceptFriendRequest(
            @RequestBody dto.AcceptRequestDTO request,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== ACCEPT FRIEND REQUEST ===");
        System.out.println("Request ID: " + request.getRequestId());
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Validate
            if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
                System.err.println("❌ Request ID is required");
                System.out.println("=============================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(FriendResponseDTO.error("Request ID is required"));
            }
            
            // Accept request
            boolean success = friendService.acceptFriendRequest(userId, request.getRequestId());
            
            if (success) {
                System.out.println("✅ Friend request accepted");
                System.out.println("=============================");
                return ResponseEntity.ok(FriendResponseDTO.success("Friend request accepted successfully"));
            } else {
                System.err.println("❌ Failed to accept friend request");
                System.out.println("=============================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(FriendResponseDTO.error("Failed to accept friend request"));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FriendResponseDTO.error("Internal server error"));
        }
    }
    
    /**
     * Reject friend request
     * POST /api/v1/friends/reject
     * Body: { "requestId": "req_456_123" }
     * 
     * @param request Reject request DTO
     * @param authorization JWT token from Authorization header
     * @return Success/error response
     */
    @PostMapping("/reject")
    public ResponseEntity<FriendResponseDTO> rejectFriendRequest(
            @RequestBody dto.AcceptRequestDTO request,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== REJECT FRIEND REQUEST ===");
        System.out.println("Request ID: " + request.getRequestId());
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Validate
            if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
                System.err.println("❌ Request ID is required");
                System.out.println("=============================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(FriendResponseDTO.error("Request ID is required"));
            }
            
            // Reject request
            boolean success = friendService.rejectFriendRequest(userId, request.getRequestId());
            
            if (success) {
                System.out.println("✅ Friend request rejected");
                System.out.println("=============================");
                return ResponseEntity.ok(FriendResponseDTO.success("Friend request rejected successfully"));
            } else {
                System.err.println("❌ Failed to reject friend request");
                System.out.println("=============================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(FriendResponseDTO.error("Failed to reject friend request"));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FriendResponseDTO.error("Internal server error"));
        }
    }
    
    /**
     * Get friend request notifications
     * GET /api/v1/friends/notifications
     * 
     * @param authorization JWT token from Authorization header
     * @return List of notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<dto.FriendNotificationDTO>> getNotifications(
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== GET FRIEND NOTIFICATIONS ===");
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Get notifications
            List<dto.FriendNotificationDTO> notifications = friendService.getNotifications(userId);
            
            System.out.println("✅ Retrieved " + notifications.size() + " notifications");
            System.out.println("=============================");
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Mark notification as read
     * DELETE /api/v1/friends/notifications/{notificationId}
     * 
     * @param notificationId Notification ID to mark as read
     * @param authorization JWT token from Authorization header
     * @return Success response
     */
    @DeleteMapping("/notifications/{notificationId}")
    public ResponseEntity<FriendResponseDTO> markNotificationAsRead(
            @PathVariable("notificationId") String notificationId,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== MARK NOTIFICATION AS READ ===");
        System.out.println("Notification ID: " + notificationId);
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Mark as read
            boolean success = friendService.markNotificationAsRead(userId, notificationId);
            
            if (success) {
                System.out.println("✅ Notification marked as read");
                System.out.println("=============================");
                return ResponseEntity.ok(FriendResponseDTO.success("Notification marked as read"));
            } else {
                System.err.println("❌ Failed to mark notification as read");
                System.out.println("=============================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(FriendResponseDTO.error("Failed to mark notification as read"));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FriendResponseDTO.error("Internal server error"));
        }
    }
}
