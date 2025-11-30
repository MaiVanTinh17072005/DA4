package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis Service
 * Handles caching user information in Redis
 */
@Service
public class RedisService {
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    
    // TTL for user cache: 5 days in seconds
    private static final int USER_CACHE_TTL = 5 * 24 * 60 * 60; // 432000 seconds
    
    // Redis key prefix for user cache
    private static final String USER_CACHE_PREFIX = "user:";
    
    @Value("${redis.host:localhost}")
    private String redisHost;
    
    @Value("${redis.port:6379}")
    private int redisPort;
    
    public RedisService() {
        // Configure Jedis pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        // Initialize Jedis pool (will use default localhost:6379 if not configured)
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);
        
        // Initialize ObjectMapper with JavaTimeModule for LocalDateTime serialization
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        System.out.println("[RedisService] Initialized with host: localhost:6379");
    }
    
    /**
     * Cache user information in Redis
     * 
     * @param user User object to cache
     */
    public void cacheUser(User user) {
        if (user == null || user.getId() == null) {
            System.out.println("[RedisService] ❌ Cannot cache null user or user without ID");
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = USER_CACHE_PREFIX + user.getId();
            
            // Convert User object to JSON
            String userJson = objectMapper.writeValueAsString(user);
            
            // Store in Redis with TTL of 10 days
            jedis.setex(key, USER_CACHE_TTL, userJson);
            
            System.out.println("[RedisService] ✓ User cached successfully:");
            System.out.println("  - User ID: " + user.getId());
            System.out.println("  - Username: " + user.getUsername());
            System.out.println("  - Email: " + user.getEmail());
            System.out.println("  - Redis Key: " + key);
            System.out.println("  - TTL: " + USER_CACHE_TTL + " seconds (5 days)");
            
        } catch (JsonProcessingException e) {
            System.out.println("[RedisService] ❌ Failed to serialize user to JSON: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[RedisService] ❌ Failed to cache user in Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get cached user from Redis
     * 
     * @param userId User ID
     * @return User object if found, null otherwise
     */
    public User getCachedUser(Long userId) {
        if (userId == null) {
            System.out.println("[RedisService] ❌ Cannot get cached user with null ID");
            return null;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = USER_CACHE_PREFIX + userId;
            String userJson = jedis.get(key);
            
            if (userJson == null) {
                System.out.println("[RedisService] ⚠ User not found in cache: " + userId);
                return null;
            }
            
            // Convert JSON to User object
            User user = objectMapper.readValue(userJson, User.class);
            
            System.out.println("[RedisService] ✓ User retrieved from cache:");
            System.out.println("  - User ID: " + user.getId());
            System.out.println("  - Username: " + user.getUsername());
            
            return user;
            
        } catch (JsonProcessingException e) {
            System.out.println("[RedisService] ❌ Failed to deserialize user from JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.out.println("[RedisService] ❌ Failed to get cached user from Redis: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Remove user from cache
     * 
     * @param userId User ID
     */
    public void removeCachedUser(Long userId) {
        if (userId == null) {
            System.out.println("[RedisService] ❌ Cannot remove cached user with null ID");
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = USER_CACHE_PREFIX + userId;
            Long deleted = jedis.del(key);
            
            if (deleted > 0) {
                System.out.println("[RedisService] ✓ User removed from cache: " + userId);
            } else {
                System.out.println("[RedisService] ⚠ User not found in cache for removal: " + userId);
            }
            
        } catch (Exception e) {
            System.out.println("[RedisService] ❌ Failed to remove cached user from Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if user exists in cache
     * 
     * @param userId User ID
     * @return true if user exists in cache, false otherwise
     */
    public boolean userExistsInCache(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = USER_CACHE_PREFIX + userId;
            return jedis.exists(key);
        } catch (Exception e) {
            System.out.println("[RedisService] ❌ Failed to check if user exists in cache: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get remaining TTL for cached user
     * 
     * @param userId User ID
     * @return TTL in seconds, -1 if key doesn't exist, -2 if key has no expiration
     */
    public long getCachedUserTTL(Long userId) {
        if (userId == null) {
            return -1;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = USER_CACHE_PREFIX + userId;
            return jedis.ttl(key);
        } catch (Exception e) {
            System.out.println("[RedisService] ❌ Failed to get TTL for cached user: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Delete user from cache (alias for removeCachedUser)
     * 
     * @param userId User ID
     */
    public void deleteUser(Long userId) {
        removeCachedUser(userId);
    }
    
    /**
     * Close Jedis pool (call on application shutdown)
     */
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            System.out.println("[RedisService] Jedis pool closed");
        }
    }
}
