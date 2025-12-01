package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.Friend;
import model.Group;
import model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import repository.FriendRepository;
import repository.GroupRepository;
import repository.MessageRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User Data Cache Service
 * Caches user's friends, groups, and messages in Redis on login
 * Security: Only caches necessary data with limited TTL
 */
@Service
public class UserDataCacheService {
    
    @Autowired
    private FriendRepository friendRepository;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    
    // TTL for cached data: 30 minutes (session timeout)
    private static final int CACHE_TTL = 30 * 60; // 1800 seconds
    
    // Redis key prefixes
    private static final String FRIENDS_KEY_PREFIX = "user:friends:";
    private static final String GROUPS_KEY_PREFIX = "user:groups:";
    private static final String MESSAGES_KEY_PREFIX = "user:messages:";
    
    // Limit for recent messages (for security and performance)
    private static final int MAX_RECENT_MESSAGES = 100;
    
    public UserDataCacheService() {
        // Configure Jedis pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        
        // Initialize Jedis pool
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);
        
        // Initialize ObjectMapper with JavaTimeModule for LocalDateTime
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        System.out.println("[UserDataCacheService] Initialized");
    }
    
    /**
     * Cache all user data on login
     * Caches: accepted friends, user's groups, recent messages
     * 
     * @param userId User ID
     */
    public void cacheUserDataOnLogin(Long userId) {
        System.out.println("[UserDataCache] ===== CACHING USER DATA =====");
        System.out.println("[UserDataCache] User ID: " + userId);
        
        try {
            // Cache friends (accepted only for security)
            cacheFriends(userId);
            
            // Cache groups
            cacheGroups(userId);
            
            // Cache recent messages (limited for security)
            cacheRecentMessages(userId);
            
            System.out.println("[UserDataCache] ✅ All user data cached successfully");
            System.out.println("[UserDataCache] ================================");
            
        } catch (Exception e) {
            System.err.println("[UserDataCache] ❌ Error caching user data: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception - caching failure shouldn't block login
        }
    }
    
    /**
     * Cache user's accepted friends only (security)
     * 
     * @param userId User ID
     */
    private void cacheFriends(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("[UserDataCache] Caching friends...");
            
            // Get accepted friends only (security: don't cache pending/blocked)
            List<Friend> friends = friendRepository.findByUserIdAndStatus(userId, "accepted");
            
            if (friends.isEmpty()) {
                System.out.println("[UserDataCache] ⚠ No accepted friends found");
                return;
            }
            
            String key = FRIENDS_KEY_PREFIX + userId;
            
            // Convert to JSON
            String friendsJson = objectMapper.writeValueAsString(friends);
            
            // Store in Redis with TTL
            jedis.setex(key, CACHE_TTL, friendsJson);
            
            System.out.println("[UserDataCache] ✅ Cached " + friends.size() + " friends");
            System.out.println("[UserDataCache]    └─ Key: " + key);
            System.out.println("[UserDataCache]    └─ TTL: " + CACHE_TTL + " seconds (30 min)");
            
        } catch (Exception e) {
            System.err.println("[UserDataCache] ❌ Error caching friends: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cache user's groups
     * 
     * @param userId User ID
     */
    private void cacheGroups(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("[UserDataCache] Caching groups...");
            
            // Get all groups where user is a member
            List<Group> groups = groupRepository.findGroupsByUserId(userId);
            
            if (groups.isEmpty()) {
                System.out.println("[UserDataCache] ⚠ No groups found");
                return;
            }
            
            String key = GROUPS_KEY_PREFIX + userId;
            
            // Convert to JSON
            String groupsJson = objectMapper.writeValueAsString(groups);
            
            // Store in Redis with TTL
            jedis.setex(key, CACHE_TTL, groupsJson);
            
            System.out.println("[UserDataCache] ✅ Cached " + groups.size() + " groups");
            System.out.println("[UserDataCache]    └─ Key: " + key);
            System.out.println("[UserDataCache]    └─ TTL: " + CACHE_TTL + " seconds (30 min)");
            
        } catch (Exception e) {
            System.err.println("[UserDataCache] ❌ Error caching groups: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cache user's recent messages (limited for security and performance)
     * Only caches last 100 messages
     * 
     * @param userId User ID
     */
    private void cacheRecentMessages(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("[UserDataCache] Caching recent messages...");
            
            // Get recent messages (sender or receiver)
            List<Message> allMessages = messageRepository.findBySenderIdOrReceiverId(userId, userId);
            
            if (allMessages.isEmpty()) {
                System.out.println("[UserDataCache] ⚠ No messages found");
                return;
            }
            
            // Limit to recent messages only (security & performance)
            List<Message> recentMessages = allMessages.stream()
                    .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                    .limit(MAX_RECENT_MESSAGES)
                    .collect(Collectors.toList());
            
            String key = MESSAGES_KEY_PREFIX + userId;
            
            // Convert to JSON
            String messagesJson = objectMapper.writeValueAsString(recentMessages);
            
            // Store in Redis with TTL
            jedis.setex(key, CACHE_TTL, messagesJson);
            
            System.out.println("[UserDataCache] ✅ Cached " + recentMessages.size() + " recent messages");
            System.out.println("[UserDataCache]    └─ Key: " + key);
            System.out.println("[UserDataCache]    └─ TTL: " + CACHE_TTL + " seconds (30 min)");
            System.out.println("[UserDataCache]    └─ Total messages: " + allMessages.size() + " (limited to " + MAX_RECENT_MESSAGES + ")");
            
        } catch (Exception e) {
            System.err.println("[UserDataCache] ❌ Error caching messages: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear all cached data for a user (called on logout)
     * 
     * @param userId User ID
     */
    public void clearUserCache(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("[UserDataCache] Clearing cache for user: " + userId);
            
            String friendsKey = FRIENDS_KEY_PREFIX + userId;
            String groupsKey = GROUPS_KEY_PREFIX + userId;
            String messagesKey = MESSAGES_KEY_PREFIX + userId;
            
            jedis.del(friendsKey, groupsKey, messagesKey);
            
            System.out.println("[UserDataCache] ✅ Cache cleared for user: " + userId);
            
        } catch (Exception e) {
            System.err.println("[UserDataCache] ❌ Error clearing cache: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get cached friends
     * 
     * @param userId User ID
     * @return List of friends or null if not cached
     */
    public List<Friend> getCachedFriends(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = FRIENDS_KEY_PREFIX + userId;
            String friendsJson = jedis.get(key);
            
            if (friendsJson == null) {
                return null;
            }
            
            return objectMapper.readValue(friendsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Friend.class));
            
        } catch (Exception e) {
            System.err.println("[UserDataCache] Error getting cached friends: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Close Jedis pool (call on application shutdown)
     */
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            System.out.println("[UserDataCache] Jedis pool closed");
        }
    }
}
