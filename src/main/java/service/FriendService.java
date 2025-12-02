package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.PendingFriendRequestDTO;
import dto.UserDTO;
import model.Friend;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import repository.FriendRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Friend Service
 * Handles friend-related business logic with Redis-based pending requests
 */
@Service
public class FriendService {
    
    @Autowired
    private FriendRepository friendRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    // Redis key patterns
    private static final String PENDING_KEY_PREFIX = "friend:pending:";
    private static final String REQUEST_KEY_PREFIX = "friend:request:";
    private static final String FRIENDS_CACHE_PREFIX = "user:friends:";
    
    // TTL
    private static final int PENDING_REQUEST_TTL = 7 * 24 * 60 * 60; // 7 days
    private static final int FRIENDS_CACHE_TTL = 30 * 60; // 30 minutes
    
    public FriendService() {
        // Configure Jedis pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        
        // Initialize Jedis pool
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);
        
        // Initialize ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        System.out.println("[FriendService] Initialized with Redis");
    }
    
    /**
     * Get friend suggestions for a user
     * Returns users who are not friends with the current user
     */
    public List<UserDTO> getFriendSuggestions(Long userId, int limit) {
        System.out.println("[FriendService] Getting friend suggestions for user: " + userId);
        
        try {
            // Get all users
            List<User> allUsers = userRepository.findAll();
            
            // Get current user's accepted friends (both directions)
            List<Friend> myFriends = friendRepository.findByUserIdAndStatus(userId, "accepted");
            List<Friend> friendsOfMe = friendRepository.findByTargetId(userId);
            
            // Collect all friend IDs (bidirectional)
            Set<Long> excludedIds = new HashSet<>();
            
            // Add friends where I am the user
            excludedIds.addAll(myFriends.stream()
                    .map(Friend::getTargetId)
                    .collect(Collectors.toSet()));
            
            // Add friends where I am the target (and status is accepted)
            excludedIds.addAll(friendsOfMe.stream()
                    .filter(f -> "accepted".equals(f.getStatus()))
                    .map(Friend::getUserId)
                    .collect(Collectors.toSet()));
            
            // Add pending requests from Redis (received by me)
            try (Jedis jedis = jedisPool.getResource()) {
                // Requests I received
                String myPendingKey = PENDING_KEY_PREFIX + userId;
                Set<String> receivedRequests = jedis.smembers(myPendingKey);
                
                for (String requestId : receivedRequests) {
                    String[] parts = requestId.split("_");
                    if (parts.length == 3) {
                        Long senderId = Long.parseLong(parts[1]);
                        excludedIds.add(senderId);
                    }
                }
                
                // Requests I sent (scan all pending requests)
                Set<String> allRequestKeys = jedis.keys(REQUEST_KEY_PREFIX + "*");
                for (String requestKey : allRequestKeys) {
                    String requestId = requestKey.replace(REQUEST_KEY_PREFIX, "");
                    String[] parts = requestId.split("_");
                    if (parts.length == 3) {
                        Long senderId = Long.parseLong(parts[1]);
                        Long receiverId = Long.parseLong(parts[2]);
                        
                        // If I sent this request, exclude the receiver
                        if (senderId.equals(userId)) {
                            excludedIds.add(receiverId);
                        }
                    }
                }
            }
            
            // Filter out current user and friends
            List<UserDTO> suggestions = allUsers.stream()
                    .filter(user -> !user.getId().equals(userId)) // Not self
                    .filter(user -> !excludedIds.contains(user.getId())) // Not already friend/pending
                    .limit(limit)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            System.out.println("[FriendService] ✅ Found " + suggestions.size() + " suggestions");
            System.out.println("[FriendService]    └─ Excluded " + excludedIds.size() + " users (friends + pending)");
            return suggestions;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error getting suggestions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Get friends list for a user (from Redis cache or DB)
     */
    public List<UserDTO> getFriends(Long userId) {
        System.out.println("[FriendService] Getting friends for user: " + userId);
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Try to get from Redis cache first
            String cacheKey = FRIENDS_CACHE_PREFIX + userId;
            String cachedFriends = jedis.get(cacheKey);
            
            if (cachedFriends != null) {
                System.out.println("[FriendService] ✅ Found friends in Redis cache");
                // Parse cached friends (List<Friend>)
                List<Friend> friends = objectMapper.readValue(
                    cachedFriends,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Friend.class)
                );
                
                // Convert to UserDTO list
                List<UserDTO> friendDTOs = new ArrayList<>();
                for (Friend friend : friends) {
                    Optional<User> userOpt = userRepository.findById(friend.getTargetId());
                    if (userOpt.isPresent()) {
                        friendDTOs.add(convertToDTO(userOpt.get()));
                    }
                }
                
                System.out.println("[FriendService] ✅ Returning " + friendDTOs.size() + " friends from cache");
                return friendDTOs;
            }
            
            // Cache miss - get from DB and update cache
            System.out.println("[FriendService] ⚠️ Cache miss - fetching from DB");
            List<Friend> friends = friendRepository.findByUserIdAndStatus(userId, "accepted");
            
            // Convert to UserDTO list
            List<UserDTO> friendDTOs = new ArrayList<>();
            for (Friend friend : friends) {
                Optional<User> userOpt = userRepository.findById(friend.getTargetId());
                if (userOpt.isPresent()) {
                    friendDTOs.add(convertToDTO(userOpt.get()));
                }
            }
            
            // Update cache
            updateFriendsCache(userId);
            
            System.out.println("[FriendService] ✅ Returning " + friendDTOs.size() + " friends from DB");
            return friendDTOs;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error getting friends: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Search users by username or email
     */
    public List<UserDTO> searchUsers(Long userId, String query) {
        System.out.println("[FriendService] Searching users for query: " + query);
        
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String lowerQuery = query.toLowerCase().trim();
            
            // Get all users
            List<User> allUsers = userRepository.findAll();
            
            // Get current user's friends
            List<Friend> acceptedFriends = friendRepository.findByUserIdAndStatus(userId, "accepted");
            Set<Long> friendIds = acceptedFriends.stream()
                    .map(Friend::getTargetId)
                    .collect(Collectors.toSet());
            
            // Search and filter
            List<UserDTO> results = allUsers.stream()
                    .filter(user -> !user.getId().equals(userId)) // Not self
                    .filter(user -> !friendIds.contains(user.getId())) // Not already friend
                    .filter(user -> 
                        user.getUsername().toLowerCase().contains(lowerQuery) ||
                        user.getEmail().toLowerCase().contains(lowerQuery)
                    )
                    .limit(20)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            System.out.println("[FriendService] ✅ Found " + results.size() + " users matching: " + query);
            return results;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error searching users: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Send friend request (save to Redis only, not DB)
     */
    public boolean sendFriendRequest(Long senderId, Long receiverId) {
        System.out.println("[FriendService] Sending friend request from " + senderId + " to " + receiverId);
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Validate users exist
            Optional<User> senderOpt = userRepository.findById(senderId);
            Optional<User> receiverOpt = userRepository.findById(receiverId);
            
            if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
                System.err.println("[FriendService] ❌ User not found");
                return false;
            }
            
            User sender = senderOpt.get();
            
            // Generate request ID
            String requestId = "req_" + senderId + "_" + receiverId;
            String requestKey = REQUEST_KEY_PREFIX + requestId;
            
            // Check if request already exists in Redis (pending)
            if (jedis.exists(requestKey)) {
                System.err.println("[FriendService] ❌ Friend request already pending in Redis");
                return false;
            }
            
            // Check if already friends in DB (accepted status)
            Optional<Friend> existing = friendRepository.findByUserIdAndTargetId(senderId, receiverId);
            if (existing.isPresent() && "accepted".equals(existing.get().getStatus())) {
                System.err.println("[FriendService] ❌ Already friends");
                return false;
            }
            
            // Check reverse direction in Redis
            String reverseRequestId = "req_" + receiverId + "_" + senderId;
            String reverseRequestKey = REQUEST_KEY_PREFIX + reverseRequestId;
            if (jedis.exists(reverseRequestKey)) {
                System.err.println("[FriendService] ❌ Reverse friend request already pending");
                return false;
            }
            
            // Create request details
            PendingFriendRequestDTO request = new PendingFriendRequestDTO(
                requestId,
                senderId,
                sender.getUsername(),
                sender.getEmail(),
                sender.getAvatarUrl(),
                sender.getStatus(),
                LocalDateTime.now().format(DATE_FORMATTER)
            );
            
            // Store request details in Redis
            String requestJson = objectMapper.writeValueAsString(request);
            jedis.setex(requestKey, PENDING_REQUEST_TTL, requestJson);
            
            // Add to receiver's pending requests set
            String pendingKey = PENDING_KEY_PREFIX + receiverId;
            jedis.sadd(pendingKey, requestId);
            jedis.expire(pendingKey, PENDING_REQUEST_TTL);
            
            System.out.println("[FriendService] ✅ Friend request sent successfully");
            System.out.println("[FriendService]    └─ Request ID: " + requestId);
            System.out.println("[FriendService]    └─ Stored in Redis with TTL: " + PENDING_REQUEST_TTL + "s");
            return true;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error sending friend request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get pending friend requests for a user
     */
    public List<PendingFriendRequestDTO> getPendingRequests(Long userId) {
        System.out.println("[FriendService] Getting pending requests for user: " + userId);
        
        try (Jedis jedis = jedisPool.getResource()) {
            String pendingKey = PENDING_KEY_PREFIX + userId;
            Set<String> requestIds = jedis.smembers(pendingKey);
            
            List<PendingFriendRequestDTO> requests = new ArrayList<>();
            
            for (String requestId : requestIds) {
                String requestKey = REQUEST_KEY_PREFIX + requestId;
                String requestJson = jedis.get(requestKey);
                
                if (requestJson != null) {
                    PendingFriendRequestDTO request = objectMapper.readValue(
                        requestJson, PendingFriendRequestDTO.class);
                    requests.add(request);
                }
            }
            
            System.out.println("[FriendService] ✅ Found " + requests.size() + " pending requests");
            return requests;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error getting pending requests: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Accept friend request
     * Creates bidirectional friendship in DB and updates Redis cache
     */
    public boolean acceptFriendRequest(Long userId, String requestId) {
        System.out.println("[FriendService] Accepting friend request: " + requestId + " by user: " + userId);
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Get request details from Redis
            String requestKey = REQUEST_KEY_PREFIX + requestId;
            String requestJson = jedis.get(requestKey);
            
            if (requestJson == null) {
                System.err.println("[FriendService] ❌ Request not found or expired");
                return false;
            }
            
            PendingFriendRequestDTO request = objectMapper.readValue(
                requestJson, PendingFriendRequestDTO.class);
            
            Long senderId = request.getSenderId();
            
            // Validate receiver
            if (!userId.equals(extractReceiverId(requestId))) {
                System.err.println("[FriendService] ❌ User not authorized to accept this request");
                return false;
            }
            
            // Create bidirectional friendship in DB
            Friend friendship1 = new Friend(senderId, userId, "accepted");
            Friend friendship2 = new Friend(userId, senderId, "accepted");
            
            friendRepository.save(friendship1);
            friendRepository.save(friendship2);
            
            System.out.println("[FriendService] ✅ Created bidirectional friendship in DB");
            
            // Update Redis cache for both users
            updateFriendsCache(senderId);
            updateFriendsCache(userId);
            
            // Remove from pending requests
            String pendingKey = PENDING_KEY_PREFIX + userId;
            jedis.srem(pendingKey, requestId);
            jedis.del(requestKey);
            
            // Create notification for sender
            createNotification(jedis, senderId, userId, "FRIEND_REQUEST_ACCEPTED", 
                "đã chấp nhận lời mời kết bạn của bạn");
            
            System.out.println("[FriendService] ✅ Friend request accepted successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error accepting friend request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Reject friend request
     */
    public boolean rejectFriendRequest(Long userId, String requestId) {
        System.out.println("[FriendService] Rejecting friend request: " + requestId);
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Validate receiver
            if (!userId.equals(extractReceiverId(requestId))) {
                System.err.println("[FriendService] ❌ User not authorized to reject this request");
                return false;
            }
            
            String requestKey = REQUEST_KEY_PREFIX + requestId;
            String requestJson = jedis.get(requestKey);
            
            if (requestJson == null) {
                System.err.println("[FriendService] ❌ Request not found or expired");
                return false;
            }
            
            PendingFriendRequestDTO request = objectMapper.readValue(
                requestJson, PendingFriendRequestDTO.class);
            
            // Remove from Redis
            String pendingKey = PENDING_KEY_PREFIX + userId;
            jedis.srem(pendingKey, requestId);
            jedis.del(requestKey);
            
            // Create notification for sender
            createNotification(jedis, request.getSenderId(), userId, "FRIEND_REQUEST_REJECTED", 
                "đã từ chối lời mời kết bạn của bạn");
            
            System.out.println("[FriendService] ✅ Friend request rejected");
            return true;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error rejecting friend request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Cancel friend request (by sender, before acceptance)
     */
    public boolean cancelFriendRequest(Long senderId, Long receiverId) {
        System.out.println("[FriendService] Cancelling friend request from " + senderId + " to " + receiverId);
        
        try (Jedis jedis = jedisPool.getResource()) {
            String requestId = "req_" + senderId + "_" + receiverId;
            String requestKey = REQUEST_KEY_PREFIX + requestId;
            
            System.out.println("[FriendService]    └─ Request ID: " + requestId);
            System.out.println("[FriendService]    └─ Request Key: " + requestKey);
            
            // Check if request exists
            boolean exists = jedis.exists(requestKey);
            System.out.println("[FriendService]    └─ Request exists in Redis: " + exists);
            
            if (!exists) {
                System.err.println("[FriendService] ❌ Request not found (may have been accepted/rejected)");
                return false;
            }
            
            // Remove from Redis
            String pendingKey = PENDING_KEY_PREFIX + receiverId;
            System.out.println("[FriendService]    └─ Removing from pending key: " + pendingKey);
            
            Long removedFromSet = jedis.srem(pendingKey, requestId);
            Long deletedKey = jedis.del(requestKey);
            
            System.out.println("[FriendService]    └─ Removed from set: " + removedFromSet);
            System.out.println("[FriendService]    └─ Deleted key: " + deletedKey);
            System.out.println("[FriendService] ✅ Friend request cancelled successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error cancelling friend request: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to cancel friend request", e);
        }
    }
    
    /**
     * Update friends cache in Redis for a user
     */
    private void updateFriendsCache(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<Friend> friends = friendRepository.findByUserIdAndStatus(userId, "accepted");
            String cacheKey = FRIENDS_CACHE_PREFIX + userId;
            String friendsJson = objectMapper.writeValueAsString(friends);
            jedis.setex(cacheKey, FRIENDS_CACHE_TTL, friendsJson);
            
            System.out.println("[FriendService] ✅ Updated friends cache for user: " + userId);
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error updating friends cache: " + e.getMessage());
        }
    }
    
    /**
     * Extract receiver ID from request ID
     */
    private Long extractReceiverId(String requestId) {
        String[] parts = requestId.split("_");
        if (parts.length == 3) {
            return Long.parseLong(parts[2]);
        }
        return null;
    }
    
    /**
     * Convert User entity to UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt().format(DATE_FORMATTER));
        return dto;
    }
    
    /**
     * Create notification for user
     */
    private void createNotification(Jedis jedis, Long targetUserId, Long fromUserId, String type, String message) {
        try {
            // Get from user info
            Optional<User> fromUserOpt = userRepository.findById(fromUserId);
            if (fromUserOpt.isEmpty()) {
                return;
            }
            
            User fromUser = fromUserOpt.get();
            String notificationId = "notif_" + System.currentTimeMillis() + "_" + targetUserId;
            
            dto.FriendNotificationDTO notification = new dto.FriendNotificationDTO(
                notificationId,
                type,
                fromUserId,
                fromUser.getUsername(),
                fromUser.getUsername() + " " + message,
                LocalDateTime.now().format(DATE_FORMATTER)
            );
            
            String notificationJson = objectMapper.writeValueAsString(notification);
            String notificationKey = "notification:" + notificationId;
            String userNotificationsKey = "user:notifications:" + targetUserId;
            
            // Store notification (TTL 24 hours)
            jedis.setex(notificationKey, 24 * 60 * 60, notificationJson);
            
            // Add to user's notifications list
            jedis.lpush(userNotificationsKey, notificationId);
            jedis.expire(userNotificationsKey, 24 * 60 * 60);
            
            System.out.println("[FriendService] ✅ Created notification for user " + targetUserId);
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error creating notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get unread notifications for user
     */
    public List<dto.FriendNotificationDTO> getNotifications(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String userNotificationsKey = "user:notifications:" + userId;
            List<String> notificationIds = jedis.lrange(userNotificationsKey, 0, -1);
            
            List<dto.FriendNotificationDTO> notifications = new ArrayList<>();
            
            for (String notificationId : notificationIds) {
                String notificationKey = "notification:" + notificationId;
                String notificationJson = jedis.get(notificationKey);
                
                if (notificationJson != null) {
                    dto.FriendNotificationDTO notification = objectMapper.readValue(
                        notificationJson, dto.FriendNotificationDTO.class);
                    notifications.add(notification);
                }
            }
            
            System.out.println("[FriendService] ✅ Retrieved " + notifications.size() + " notifications for user " + userId);
            return notifications;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error getting notifications: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Mark notification as read and remove it
     */
    public boolean markNotificationAsRead(Long userId, String notificationId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String userNotificationsKey = "user:notifications:" + userId;
            String notificationKey = "notification:" + notificationId;
            
            // Remove from user's list
            jedis.lrem(userNotificationsKey, 1, notificationId);
            
            // Delete notification
            jedis.del(notificationKey);
            
            System.out.println("[FriendService] ✅ Marked notification as read: " + notificationId);
            return true;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error marking notification as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Remove friend (delete bidirectional friendship)
     */
    public boolean removeFriend(Long userId, Long friendId) {
        System.out.println("[FriendService] Removing friend: userId=" + userId + ", friendId=" + friendId);
        
        try {
            // Find and delete both directions of friendship
            Optional<Friend> friendship1 = friendRepository.findByUserIdAndTargetId(userId, friendId);
            Optional<Friend> friendship2 = friendRepository.findByUserIdAndTargetId(friendId, userId);
            
            if (friendship1.isEmpty() && friendship2.isEmpty()) {
                System.err.println("[FriendService] ❌ Friendship not found");
                return false;
            }
            
            // Delete both directions
            friendship1.ifPresent(friend -> {
                friendRepository.delete(friend);
                System.out.println("[FriendService] ✅ Deleted friendship: " + userId + " -> " + friendId);
            });
            
            friendship2.ifPresent(friend -> {
                friendRepository.delete(friend);
                System.out.println("[FriendService] ✅ Deleted friendship: " + friendId + " -> " + userId);
            });
            
            // Update Redis cache for both users
            updateFriendsCache(userId);
            updateFriendsCache(friendId);
            
            System.out.println("[FriendService] ✅ Friend removed successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("[FriendService] ❌ Error removing friend: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
