package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.GroupDTO;
import model.Group;
import model.GroupMember;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.GroupRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Group Service
 * Handles group-related business logic with Redis caching
 */
@Service
public class GroupService {
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private repository.GroupMemberRepository groupMemberRepository;
    
    @Autowired
    private RedisService redisService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    // Redis key patterns
    private static final String USER_GROUPS_PREFIX = "user:groups:";
    private static final String GROUP_DETAILS_PREFIX = "group:details:";
    
    // TTL: 3 days in seconds
    private static final int GROUPS_CACHE_TTL = 3 * 24 * 60 * 60; // 259200 seconds
    
    /**
     * Get all groups for a user
     * Cached in Redis
     */
    public List<GroupDTO> getGroupsForUser(Long userId) {
        System.out.println("[GroupService] Getting groups for user: " + userId);
        
        try {
            // Try to get from Redis cache first
            String cacheKey = USER_GROUPS_PREFIX + userId;
            List<GroupDTO> cachedGroups = redisService.getCachedList(cacheKey, GroupDTO.class);
            
            if (cachedGroups != null) {
                System.out.println("[GroupService] ✅ Found " + cachedGroups.size() + " groups in Redis cache");
                return cachedGroups;
            }
            
            // Cache miss - get from DB
            System.out.println("[GroupService] ⚠️ Cache miss - fetching from DB");
            List<Group> groups = groupRepository.findGroupsByUserId(userId);
            
            // Convert to DTOs
            List<GroupDTO> groupDTOs = groups.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            // Update cache
            redisService.cacheObject(cacheKey, groupDTOs, GROUPS_CACHE_TTL);
            
            System.out.println("[GroupService] ✅ Returning " + groupDTOs.size() + " groups from DB");
            return groupDTOs;
            
        } catch (Exception e) {
            System.err.println("[GroupService] ❌ Error getting groups: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Get group details by ID
     * Cached in Redis
     */
    public GroupDTO getGroupDetails(Long groupId) {
        System.out.println("[GroupService] Getting group details: " + groupId);
        
        try {
            // Try to get from Redis cache first
            String cacheKey = GROUP_DETAILS_PREFIX + groupId;
            GroupDTO cachedGroup = redisService.getCachedObject(cacheKey, GroupDTO.class);
            
            if (cachedGroup != null) {
                System.out.println("[GroupService] ✅ Found group in Redis cache");
                return cachedGroup;
            }
            
            // Cache miss - get from DB
            System.out.println("[GroupService] ⚠️ Cache miss - fetching from DB");
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            
            if (groupOpt.isEmpty()) {
                System.err.println("[GroupService] ❌ Group not found: " + groupId);
                return null;
            }
            
            Group group = groupOpt.get();
            GroupDTO groupDTO = convertToDTO(group);
            
            // Update cache
            redisService.cacheObject(cacheKey, groupDTO, GROUPS_CACHE_TTL);
            
            System.out.println("[GroupService] ✅ Returning group from DB");
            return groupDTO;
            
        } catch (Exception e) {
            System.err.println("[GroupService] ❌ Error getting group details: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Create a new group
     * Properly handles database schema:
     * 1. Creates group in 'groups' table
     * 2. Adds owner to 'group_member' with role ADMIN
     * 3. Adds initial members to 'group_member' with role MEMBER
     * 4. Updates member_count
     * Invalidates user's groups cache
     */
    public GroupDTO createGroup(GroupDTO groupDTO) {
        System.out.println("[GroupService] Creating group: " + groupDTO.getName());
        
        try {
            // Create Group entity
            Group group = new Group();
            group.setName(groupDTO.getName());
            group.setOwnerId(groupDTO.getOwnerId());
            group.setDescription(groupDTO.getDescription());
            group.setCreatedAt(LocalDateTime.now());
            
            // Calculate member count (owner + initial members)
            int initialMemberCount = 1; // Owner
            if (groupDTO.getMemberIds() != null) {
                initialMemberCount += groupDTO.getMemberIds().size();
            }
            group.setMemberCount(initialMemberCount);
            
            // Save group to DB
            Group savedGroup = groupRepository.save(group);
            System.out.println("[GroupService] ✅ Group created: ID=" + savedGroup.getGroupId());
            
            // Add owner as ADMIN to group_member table
            GroupMember ownerMember = new GroupMember();
            ownerMember.setGroupId(savedGroup.getGroupId());
            ownerMember.setUserId(groupDTO.getOwnerId());
            ownerMember.setRole("ADMIN");
            ownerMember.setJoinedAt(LocalDateTime.now());
            groupMemberRepository.save(ownerMember);
            System.out.println("[GroupService] ✅ Owner added as ADMIN");
            
            // Add initial members to group_member table
            if (groupDTO.getMemberIds() != null && !groupDTO.getMemberIds().isEmpty()) {
                for (Long memberId : groupDTO.getMemberIds()) {
                    // Skip if member is the owner (already added)
                    if (memberId.equals(groupDTO.getOwnerId())) {
                        continue;
                    }
                    
                    GroupMember member = new GroupMember();
                    member.setGroupId(savedGroup.getGroupId());
                    member.setUserId(memberId);
                    member.setRole("MEMBER");
                    member.setJoinedAt(LocalDateTime.now());
                    groupMemberRepository.save(member);
                    
                    System.out.println("[GroupService] ✅ Added member: " + memberId);
                }
            }
            
            // Invalidate owner's groups cache
            redisService.removeCachedObject(USER_GROUPS_PREFIX + groupDTO.getOwnerId());
            
            // Also invalidate each member's groups cache
            if (groupDTO.getMemberIds() != null) {
                for (Long memberId : groupDTO.getMemberIds()) {
                    redisService.removeCachedObject(USER_GROUPS_PREFIX + memberId);
                }
            }
            
            System.out.println("[GroupService] ✅ Group created successfully with " + initialMemberCount + " members");
            
            // Return DTO
            return convertToDTO(savedGroup);
            
        } catch (Exception e) {
            System.err.println("[GroupService] ❌ Error creating group: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Update group details
     * Invalidates group cache
     */
    public GroupDTO updateGroup(Long groupId, GroupDTO groupDTO) {
        System.out.println("[GroupService] Updating group: " + groupId);
        
        try {
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            
            if (groupOpt.isEmpty()) {
                System.err.println("[GroupService] ❌ Group not found: " + groupId);
                return null;
            }
            
            Group group = groupOpt.get();
            
            // Update fields
            if (groupDTO.getName() != null) {
                group.setName(groupDTO.getName());
            }
            if (groupDTO.getDescription() != null) {
                group.setDescription(groupDTO.getDescription());
            }
            
            // Save to DB
            Group updatedGroup = groupRepository.save(group);
            
            // Invalidate caches
            redisService.removeCachedObject(GROUP_DETAILS_PREFIX + groupId);
            // Note: We don't invalidate user groups cache here as it would require 
            // fetching all members, which is expensive. Cache will expire naturally.
            
            System.out.println("[GroupService] ✅ Group updated successfully");
            
            // Return DTO
            return convertToDTO(updatedGroup);
            
        } catch (Exception e) {
            System.err.println("[GroupService] ❌ Error updating group: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Delete a group
     * Invalidates caches
     */
    public boolean deleteGroup(Long groupId) {
        System.out.println("[GroupService] Deleting group: " + groupId);
        
        try {
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            
            if (groupOpt.isEmpty()) {
                System.err.println("[GroupService] ❌ Group not found: " + groupId);
                return false;
            }
            
            // Delete from DB
            groupRepository.deleteById(groupId);
            
            // Invalidate caches
            redisService.removeCachedObject(GROUP_DETAILS_PREFIX + groupId);
            
            System.out.println("[GroupService] ✅ Group deleted successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("[GroupService] ❌ Error deleting group: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Update groups cache for a user
     * Manually refresh cache
     */
    public void updateGroupsCache(Long userId) {
        try {
            List<Group> groups = groupRepository.findGroupsByUserId(userId);
            List<GroupDTO> groupDTOs = groups.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            String cacheKey = USER_GROUPS_PREFIX + userId;
            redisService.cacheObject(cacheKey, groupDTOs, GROUPS_CACHE_TTL);
            
            System.out.println("[GroupService] ✅ Updated groups cache for user: " + userId);
        } catch (Exception e) {
            System.err.println("[GroupService] ❌ Error updating groups cache: " + e.getMessage());
        }
    }
    
    /**
     * Convert Group entity to GroupDTO
     */
    private GroupDTO convertToDTO(Group group) {
        GroupDTO dto = new GroupDTO();
        dto.setGroupId(group.getGroupId());
        dto.setName(group.getName());
        dto.setOwnerId(group.getOwnerId());
        dto.setDescription(group.getDescription());
        dto.setMemberCount(group.getMemberCount());
        dto.setCreatedAt(group.getCreatedAt().format(DATE_FORMATTER));
        
        // Get owner username
        Optional<User> ownerOpt = userRepository.findById(group.getOwnerId());
        if (ownerOpt.isPresent()) {
            dto.setOwnerUsername(ownerOpt.get().getUsername());
        }
        
        return dto;
    }
}
