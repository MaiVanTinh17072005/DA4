package com.example.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import dto.MessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service to manage message queue in Redis using Hash structure
 * Key format: pending_messages:{userId}
 * Hash field: messageId
 * Hash value: JSON string of MessageDTO
 */
@Service
public class RedisMessageQueueService {
    
    private static final String QUEUE_KEY_PREFIX = "pending_messages:";
    
    @Autowired
    private JedisPool jedisPool;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Add message to Redis queue
     */
    public void queueMessage(MessageDTO message) {
        try (Jedis jedis = jedisPool.getResource()) {
            Long senderId = message.getSenderId();
            String messageId = String.valueOf(message.getMsgId());
            String queueKey = QUEUE_KEY_PREFIX + senderId;
            
            // Convert MessageDTO to JSON
            String messageJson = objectMapper.writeValueAsString(message);
            
            // Store in Redis Hash: HSET pending_messages:{userId} {messageId} {json}
            jedis.hset(queueKey, messageId, messageJson);
            
            System.out.println("[RedisQueue] ➕ Queued message " + messageId + " for user " + senderId);
            
        } catch (Exception e) {
            System.err.println("[RedisQueue] ❌ Failed to queue message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to queue message to Redis", e);
        }
    }
    
    /**
     * Get all pending messages for a specific user
     */
    public List<MessageDTO> getPendingMessages(Long userId) {
        List<MessageDTO> messages = new ArrayList<>();
        
        try (Jedis jedis = jedisPool.getResource()) {
            String queueKey = QUEUE_KEY_PREFIX + userId;
            
            // Get all messages from hash: HGETALL pending_messages:{userId}
            Map<String, String> messageMap = jedis.hgetAll(queueKey);
            
            for (String messageJson : messageMap.values()) {
                try {
                    MessageDTO message = objectMapper.readValue(messageJson, MessageDTO.class);
                    messages.add(message);
                } catch (Exception e) {
                    System.err.println("[RedisQueue] ⚠️ Failed to parse message: " + e.getMessage());
                }
            }
            
            System.out.println("[RedisQueue] 📂 Retrieved " + messages.size() + " pending messages for user " + userId);
            
        } catch (Exception e) {
            System.err.println("[RedisQueue] ❌ Failed to get pending messages: " + e.getMessage());
            e.printStackTrace();
        }
        
        return messages;
    }
    
    /**
     * Get ALL pending messages from all users
     */
    public List<MessageDTO> getAllPendingMessages() {
        List<MessageDTO> allMessages = new ArrayList<>();
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Get all keys matching pattern: pending_messages:*
            var keys = jedis.keys(QUEUE_KEY_PREFIX + "*");
            
            for (String key : keys) {
                // Get all messages from this user's hash
                Map<String, String> messageMap = jedis.hgetAll(key);
                
                for (String messageJson : messageMap.values()) {
                    try {
                        MessageDTO message = objectMapper.readValue(messageJson, MessageDTO.class);
                        allMessages.add(message);
                    } catch (Exception e) {
                        System.err.println("[RedisQueue] ⚠️ Failed to parse message: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("[RedisQueue] 📂 Retrieved " + allMessages.size() + " total pending messages");
            
        } catch (Exception e) {
            System.err.println("[RedisQueue] ❌ Failed to get all pending messages: " + e.getMessage());
            e.printStackTrace();
        }
        
        return allMessages;
    }
    
    /**
     * Remove a specific message from queue after successful sync
     */
    public void removeMessage(Long userId, String messageId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String queueKey = QUEUE_KEY_PREFIX + userId;
            
            // Remove from hash: HDEL pending_messages:{userId} {messageId}
            jedis.hdel(queueKey, messageId);
            
            System.out.println("[RedisQueue] ✅ Removed message " + messageId + " for user " + userId);
            
        } catch (Exception e) {
            System.err.println("[RedisQueue] ❌ Failed to remove message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear all pending messages for a user
     */
    public void clearUserQueue(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String queueKey = QUEUE_KEY_PREFIX + userId;
            
            // Delete entire hash: DEL pending_messages:{userId}
            jedis.del(queueKey);
            
            System.out.println("[RedisQueue] 🗑️ Cleared queue for user " + userId);
            
        } catch (Exception e) {
            System.err.println("[RedisQueue] ❌ Failed to clear queue: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get count of pending messages for a user
     */
    public long getPendingCount(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String queueKey = QUEUE_KEY_PREFIX + userId;
            return jedis.hlen(queueKey);
        } catch (Exception e) {
            System.err.println("[RedisQueue] ❌ Failed to get count: " + e.getMessage());
            return 0;
        }
    }
}
