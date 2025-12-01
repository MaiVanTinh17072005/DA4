package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.P2PInfoRequest;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * P2P Service
 * Handles P2P connection information storage and retrieval in Redis
 */
@Service
public class P2PService {
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    
    // TTL for P2P info: 30 minutes (session timeout)
    private static final int P2P_INFO_TTL = 30 * 60; // 1800 seconds
    
    // Redis key prefix for P2P info
    private static final String P2P_INFO_PREFIX = "p2p:user:";
    
    public P2PService() {
        // Configure Jedis pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        // Initialize Jedis pool
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);
        
        // Initialize ObjectMapper
        this.objectMapper = new ObjectMapper();
        
        System.out.println("[P2PService] Initialized with Redis at localhost:6379");
    }
    
    /**
     * Register or update P2P information for a user
     * If user already has P2P info, it will be updated
     * 
     * @param request P2PInfoRequest containing userId, ipAddress, tcpPort, udpPort
     * @return true if successful, false otherwise
     */
    public boolean registerP2PInfo(P2PInfoRequest request) {
        if (request == null || request.getUserId() == null) {
            System.out.println("[P2PService] ❌ Cannot register null request or request without userId");
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = P2P_INFO_PREFIX + request.getUserId();
            
            // Create P2P info map
            Map<String, String> p2pInfo = new HashMap<>();
            p2pInfo.put("userId", String.valueOf(request.getUserId()));
            p2pInfo.put("ipAddress", request.getIpAddress());
            p2pInfo.put("tcpPort", String.valueOf(request.getTcpPort()));
            p2pInfo.put("udpPort", String.valueOf(request.getUdpPort()));
            p2pInfo.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // Store as hash in Redis
            jedis.hset(key, p2pInfo);
            
            // Set TTL
            jedis.expire(key, P2P_INFO_TTL);
            
            System.out.println("[P2PService] ✅ P2P info registered/updated successfully:");
            System.out.println("  - User ID: " + request.getUserId());
            System.out.println("  - IP Address: " + request.getIpAddress());
            System.out.println("  - TCP Port: " + request.getTcpPort());
            System.out.println("  - UDP Port: " + request.getUdpPort());
            System.out.println("  - Redis Key: " + key);
            System.out.println("  - TTL: " + P2P_INFO_TTL + " seconds (30 minutes)");
            
            return true;
            
        } catch (Exception e) {
            System.out.println("[P2PService] ❌ Failed to register P2P info: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get P2P information for a user
     * 
     * @param userId User ID
     * @return P2PInfoRequest if found, null otherwise
     */
    public P2PInfoRequest getP2PInfo(Long userId) {
        if (userId == null) {
            System.out.println("[P2PService] ❌ Cannot get P2P info with null userId");
            return null;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = P2P_INFO_PREFIX + userId;
            
            // Get all fields from hash
            Map<String, String> p2pInfo = jedis.hgetAll(key);
            
            if (p2pInfo == null || p2pInfo.isEmpty()) {
                System.out.println("[P2PService] ⚠ P2P info not found for user: " + userId);
                return null;
            }
            
            // Convert to P2PInfoRequest
            P2PInfoRequest request = new P2PInfoRequest();
            request.setUserId(Long.parseLong(p2pInfo.get("userId")));
            request.setIpAddress(p2pInfo.get("ipAddress"));
            request.setTcpPort(Integer.parseInt(p2pInfo.get("tcpPort")));
            request.setUdpPort(Integer.parseInt(p2pInfo.get("udpPort")));
            
            System.out.println("[P2PService] ✅ P2P info retrieved:");
            System.out.println("  - User ID: " + request.getUserId());
            System.out.println("  - IP Address: " + request.getIpAddress());
            System.out.println("  - TCP Port: " + request.getTcpPort());
            System.out.println("  - UDP Port: " + request.getUdpPort());
            
            return request;
            
        } catch (Exception e) {
            System.out.println("[P2PService] ❌ Failed to get P2P info: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Remove P2P information for a user
     * 
     * @param userId User ID
     * @return true if removed, false otherwise
     */
    public boolean removeP2PInfo(Long userId) {
        if (userId == null) {
            System.out.println("[P2PService] ❌ Cannot remove P2P info with null userId");
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = P2P_INFO_PREFIX + userId;
            Long deleted = jedis.del(key);
            
            if (deleted > 0) {
                System.out.println("[P2PService] ✅ P2P info removed for user: " + userId);
                return true;
            } else {
                System.out.println("[P2PService] ⚠ P2P info not found for removal: " + userId);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("[P2PService] ❌ Failed to remove P2P info: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if P2P info exists for a user
     * 
     * @param userId User ID
     * @return true if exists, false otherwise
     */
    public boolean p2pInfoExists(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = P2P_INFO_PREFIX + userId;
            return jedis.exists(key);
        } catch (Exception e) {
            System.out.println("[P2PService] ❌ Failed to check if P2P info exists: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Close Jedis pool (call on application shutdown)
     */
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            System.out.println("[P2PService] Jedis pool closed");
        }
    }
}
