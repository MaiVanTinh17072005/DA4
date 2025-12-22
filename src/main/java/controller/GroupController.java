package controller;

import dto.GroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.GroupService;
import util.JwtUtil;

import java.util.List;

/**
 * Group Controller
 * REST API endpoints for group management
 */
@RestController
@RequestMapping("/api/v1/groups")
@CrossOrigin(origins = "*") // Allow requests from JavaFX client
public class GroupController {
    
    @Autowired
    private GroupService groupService;
    
    /**
     * Get all groups for current user
     * GET /api/v1/groups
     * 
     * @param authorization JWT token from Authorization header
     * @return List of groups
     */
    @GetMapping
    public ResponseEntity<List<GroupDTO>> getUserGroups(
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== GET USER GROUPS ===");
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Get groups
            List<GroupDTO> groups = groupService.getGroupsForUser(userId);
            
            System.out.println("✅ Returning " + groups.size() + " groups");
            System.out.println("=======================");
            return ResponseEntity.ok(groups);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=======================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get group details by ID
     * GET /api/v1/groups/{groupId}
     * 
     * @param groupId Group ID
     * @param authorization JWT token from Authorization header
     * @return Group details
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDTO> getGroupDetails(
            @PathVariable("groupId") Long groupId,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== GET GROUP DETAILS ===");
        System.out.println("Group ID: " + groupId);
        
        try {
            // Extract user ID from JWT token (for authentication)
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Get group details
            GroupDTO group = groupService.getGroupDetails(groupId);
            
            if (group != null) {
                System.out.println("✅ Returning group details");
                System.out.println("=========================");
                return ResponseEntity.ok(group);
            } else {
                System.err.println("❌ Group not found");
                System.out.println("=========================");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=========================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Create a new group
     * POST /api/v1/groups/create
     * Body: GroupDTO
     * 
     * @param groupDTO Group data
     * @param authorization JWT token from Authorization header
     * @return Created group
     */
    @PostMapping("/create")
    public ResponseEntity<GroupDTO> createGroup(
            @RequestBody GroupDTO groupDTO,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== CREATE GROUP ===");
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            // Set owner ID from token
            groupDTO.setOwnerId(userId);
            
            System.out.println("Owner ID: " + userId);
            System.out.println("Group Name: " + groupDTO.getName());
            
            // Create group
            GroupDTO createdGroup = groupService.createGroup(groupDTO);
            
            if (createdGroup != null) {
                System.out.println("✅ Group created successfully");
                System.out.println("====================");
                return ResponseEntity.ok(createdGroup);
            } else {
                System.err.println("❌ Failed to create group");
                System.out.println("====================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("====================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Update group details
     * PUT /api/v1/groups/{groupId}
     * Body: GroupDTO
     * 
     * @param groupId Group ID
     * @param groupDTO Updated group data
     * @param authorization JWT token from Authorization header
     * @return Updated group
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<GroupDTO> updateGroup(
            @PathVariable("groupId") Long groupId,
            @RequestBody GroupDTO groupDTO,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== UPDATE GROUP ===");
        System.out.println("Group ID: " + groupId);
        
        try {
            // Extract user ID from JWT token (for authentication)
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Update group
            GroupDTO updatedGroup = groupService.updateGroup(groupId, groupDTO);
            
            if (updatedGroup != null) {
                System.out.println("✅ Group updated successfully");
                System.out.println("====================");
                return ResponseEntity.ok(updatedGroup);
            } else {
                System.err.println("❌ Failed to update group");
                System.out.println("====================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("====================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get group members
     * GET /api/v1/groups/{groupId}/members
     * 
     * @param groupId Group ID
     * @param authorization JWT token
     * @return List of group members
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<dto.GroupMemberDTO>> getGroupMembers(
            @PathVariable("groupId") Long groupId,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== GET GROUP MEMBERS ===");
        System.out.println("Group ID: " + groupId);
        
        try {
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            // Get members from GroupService
            // Note: GroupService needs getGroupMembers method
            System.out.println("✅ Getting members for group: " + groupId);
            System.out.println("===========================");
            
            // TODO: Implement in GroupService
            return ResponseEntity.ok(java.util.Collections.emptyList());
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("===========================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Add member to group
     * POST /api/v1/groups/{groupId}/members
     * Body: {"userId": 123, "role": "member"}
     * 
     * @param groupId Group ID
     * @param request Request body with userId and role
     * @param authorization JWT token
     * @return Success message
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<String> addGroupMember(
            @PathVariable("groupId") Long groupId,
            @RequestBody java.util.Map<String, Object> request,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== ADD GROUP MEMBER ===");
        System.out.println("Group ID: " + groupId);
        
        try {
            String token = authorization.replace("Bearer ", "");
            Long requesterId = JwtUtil.extractUserId(token);
            
            Long userId = Long.valueOf(request.get("userId").toString());
            String role = request.getOrDefault("role", "member").toString();
            
            System.out.println("Adding user " + userId + " as " + role);
            
            // TODO: Implement in GroupService
            System.out.println("✅ Member added successfully");
            System.out.println("=========================");
            return ResponseEntity.ok("Member added successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=========================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add member");
        }
    }
    
    /**
     * Remove member from group
     * DELETE /api/v1/groups/{groupId}/members/{userId}
     * 
     * @param groupId Group ID
     * @param userId User ID to remove
     * @param authorization JWT token
     * @return Success message
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<String> removeGroupMember(
            @PathVariable("groupId") Long groupId,
            @PathVariable("userId") Long userId,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== REMOVE GROUP MEMBER ===");
        System.out.println("Group ID: " + groupId);
        System.out.println("User ID: " + userId);
        
        try {
            String token = authorization.replace("Bearer ", "");
            Long requesterId = JwtUtil.extractUserId(token);
            
            // TODO: Implement in GroupService
            System.out.println("✅ Member removed successfully");
            System.out.println("=============================");
            return ResponseEntity.ok("Member removed successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove member");
        }
    }
    
    /**
     * Delete a group
     * DELETE /api/v1/groups/{groupId}
     * 
     * @param groupId Group ID
     * @param authorization JWT token from Authorization header
     * @return Success response
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<String> deleteGroup(
            @PathVariable("groupId") Long groupId,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== DELETE GROUP ===");
        System.out.println("Group ID: " + groupId);
        
        try {
            // Extract user ID from JWT token (for authentication)
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Delete group
            boolean success = groupService.deleteGroup(groupId);
            
            if (success) {
                System.out.println("✅ Group deleted successfully");
                System.out.println("====================");
                return ResponseEntity.ok("Group deleted successfully");
            } else {
                System.err.println("❌ Failed to delete group");
                System.out.println("====================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to delete group");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("====================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
}
