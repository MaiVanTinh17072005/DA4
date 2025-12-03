package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.CallHistoryDTO;
import dto.P2PInfoDTO;
import dto.P2PInfoRequest;
import model.CallHistory;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import repository.CallHistoryRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * P2P Service
 * Handles P2P connection information storage and retrieval in Redis
 * Also handles call history with Redis caching
 */
@Service
public class P2PService {
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private CallHistoryRepository callHistoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisService redisService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    // TTL for P2P info: 30 minutes (session timeout)
    private static final int P2P_INFO_TTL = 30 * 60; // 1800 seconds
    
    // Redis key prefix for P2P info
    private static final String P2P_INFO_KEY_PREFIX = "p2p:info:";
    
    // TTL for call history: 3 days
    private static final int CALL_HISTORY_TTL = 3 * 24 * 60 * 60; // 259200 seconds
    
    // Redis key prefix for P2P info
    private static final String P2P_INFO_PREFIX = "p2p:user:";
    
    // Redis key prefix for call history
    private static final String CALL_HISTORY_PREFIX = "user:calls:";
    
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
        
        // Initialize ObjectMapper with JavaTimeModule
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
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
     * Get call history for a user
     * Cached in Redis
     */
    public List<CallHistoryDTO> getCallHistory(Long userId) {
        System.out.println("[P2PService] Getting call history for user: " + userId);
        
        try {
            // Try to get from Redis cache first
            String cacheKey = CALL_HISTORY_PREFIX + userId;
            List<CallHistoryDTO> cachedCalls = redisService.getCachedList(cacheKey, CallHistoryDTO.class);
            
            if (cachedCalls != null) {
                System.out.println("[P2PService] ✅ Found " + cachedCalls.size() + " calls in Redis cache");
                return cachedCalls;
            }
            
            // Cache miss - get from DB
            System.out.println("[P2PService] ⚠️ Cache miss - fetching from DB");
            List<CallHistory> calls = callHistoryRepository.findByCallerIdOrReceiverId(userId, userId);
            
            // Convert to DTOs
            List<CallHistoryDTO> callDTOs = calls.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            // Update cache
            redisService.cacheObject(cacheKey, callDTOs, CALL_HISTORY_TTL);
            
            System.out.println("[P2PService] ✅ Returning " + callDTOs.size() + " calls from DB");
            return callDTOs;
            
        } catch (Exception e) {
            System.err.println("[P2PService] ❌ Error getting call history: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Save call history record
     * Invalidates cache for both caller and receiver
     */
    public CallHistoryDTO saveCallHistory(CallHistoryDTO callDTO) {
        System.out.println("[P2PService] Saving call history: " + callDTO.getType());
        
        try {
            // Create CallHistory entity
            CallHistory call = new CallHistory();
            call.setCallerId(callDTO.getCallerId());
            call.setReceiverId(callDTO.getReceiverId());
            call.setType(callDTO.getType());
            call.setStartTime(LocalDateTime.parse(callDTO.getStartTime(), DATE_FORMATTER));
            
            if (callDTO.getEndTime() != null) {
                call.setEndTime(LocalDateTime.parse(callDTO.getEndTime(), DATE_FORMATTER));
            }
            
            call.setDuration(callDTO.getDuration());
            call.setSuccess(callDTO.getSuccess() != null ? callDTO.getSuccess() : false);
            
            // Save to DB
            CallHistory savedCall = callHistoryRepository.save(call);
            
            // Invalidate caches for both users
            redisService.removeCachedObject(CALL_HISTORY_PREFIX + callDTO.getCallerId());
            redisService.removeCachedObject(CALL_HISTORY_PREFIX + callDTO.getReceiverId());
            
            System.out.println("[P2PService] ✅ Call history saved successfully: ID=" + savedCall.getCallId());
            
            // Return DTO
            return convertToDTO(savedCall);
            
        } catch (Exception e) {
            System.err.println("[P2PService] ❌ Error saving call history: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Update call history cache for a user
     * Manually refresh cache
     */
    public void updateCallHistoryCache(Long userId) {
        try {
            List<CallHistory> calls = callHistoryRepository.findByCallerIdOrReceiverId(userId, userId);
            List<CallHistoryDTO> callDTOs = calls.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            String cacheKey = CALL_HISTORY_PREFIX + userId;
            redisService.cacheObject(cacheKey, callDTOs, CALL_HISTORY_TTL);
            
            System.out.println("[P2PService] ✅ Updated call history cache for user: " + userId);
        } catch (Exception e) {
            System.err.println("[P2PService] ❌ Error updating call history cache: " + e.getMessage());
        }
    }
    
    /**
     * Convert CallHistory entity to CallHistoryDTO
     */
    private CallHistoryDTO convertToDTO(CallHistory call) {
        CallHistoryDTO dto = new CallHistoryDTO();
        dto.setCallId(call.getCallId());
        dto.setCallerId(call.getCallerId());
        dto.setReceiverId(call.getReceiverId());
        dto.setType(call.getType());
        dto.setStartTime(call.getStartTime().format(DATE_FORMATTER));
        
        if (call.getEndTime() != null) {
            dto.setEndTime(call.getEndTime().format(DATE_FORMATTER));
        }
        
        dto.setDuration(call.getDuration());
        dto.setSuccess(call.getSuccess());
        
        // Get caller username
        Optional<User> callerOpt = userRepository.findById(call.getCallerId());
        if (callerOpt.isPresent()) {
            dto.setCallerUsername(callerOpt.get().getUsername());
        }
        
        // Get receiver username
        Optional<User> receiverOpt = userRepository.findById(call.getReceiverId());
        if (receiverOpt.isPresent()) {
            dto.setReceiverUsername(receiverOpt.get().getUsername());
        }
        
        return dto;
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
