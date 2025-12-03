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
