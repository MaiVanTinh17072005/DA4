package com.example.service;

import com.example.api.dto.MessageDTO;
import com.example.api.dto.UserDTO;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;

/**
 * Local Redis Service for Peer Offline Storage
 * Stores encrypted messages, user profile, friends when server is offline
 */
public class LocalRedisService {
    
    private static LocalRedisService instance;
    private JedisPool jedisPool;
    private Gson gson;
    
    private LocalRedisService() {
        this.gson = new Gson();
        initRedisPool();
    }
    
    public static synchronized LocalRedisService getInstance() {
        if (instance == null) {
            instance = new LocalRedisService();
        }
        return instance;
    }
    
    private void initRedisPool() {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(10);
            config.setMaxIdle(5);
            config.setMinIdle(1);
            config.setTestOnBorrow(true);
            
            // Connect to local Redis (Docker container on localhost:6379)
//            jedisPool = new JedisPool(config, "localhost", 6379);
            jedisPool = new JedisPool(config, "192.168.240.58", 6379);
            
            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                String pong = jedis.ping();
                System.out.println("[LocalRedis] ✅ Connected to local Redis: " + pong);
            }
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to connect to local Redis: " + e.getMessage());
            System.err.println("[LocalRedis] ⚠️ Make sure Redis is running on localhost:6379");
        }
    }
    
    // ===== USER PROFILE =====
    
    public void saveUserProfile(UserDTO user) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "user:" + user.getId();
            Map<String, String> userData = new HashMap<>();
            userData.put("id", String.valueOf(user.getId()));
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            if (user.getAvatarUrl() != null) {
                userData.put("avatar", user.getAvatarUrl());
            }
            
            jedis.hset(key, userData);
            System.out.println("[LocalRedis] 💾 Saved user profile: " + user.getUsername());
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to save user profile: " + e.getMessage());
        }
    }
    
    public UserDTO getUserProfile(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "user:" + userId;
            Map<String, String> userData = jedis.hgetAll(key);
            
            if (userData.isEmpty()) {
                return null;
            }
            
            UserDTO user = new UserDTO();
            user.setId(Long.parseLong(userData.get("id")));
            user.setUsername(userData.get("username"));
            user.setEmail(userData.get("email"));
            user.setAvatarUrl(userData.get("avatar"));
            
            return user;
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to get user profile: " + e.getMessage());
            return null;
        }
    }
    
    // ===== PENDING MESSAGES (ENCRYPTED) =====
    
    public void queueMessage(MessageDTO message) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "pending_messages:" + message.getSenderId();
            String messageId = String.valueOf(message.getMsgId());
            String messageJson = gson.toJson(message);
            
            // ✅ ENCRYPTION STATUS CHECK (Allow both encrypted and plain text)
            if (message.getAesEncrypted() == null || !message.getAesEncrypted()) {
                System.out.println("\n========== [LocalRedis] ⚠️ PLAIN TEXT MESSAGE QUEUED ==========");
                System.out.println("[LocalRedis] Message ID: " + messageId);
                System.out.println("[LocalRedis] SenderId: " + message.getSenderId());
                System.out.println("[LocalRedis] ReceiverId: " + message.getReceiverId());
                System.out.println("[LocalRedis] Encrypted: false");
                System.out.println("[LocalRedis] Content (plain): " + (message.getContent() != null ? message.getContent().substring(0, Math.min(50, message.getContent().length())) : "null"));
                System.out.println("[LocalRedis] ⚠️ Message is NOT encrypted - server can read content");
                System.out.println("================================================================\n");
            } else {
                System.out.println("\n========== [LocalRedis] ✅ ENCRYPTED MESSAGE QUEUED ==========");
                System.out.println("[LocalRedis] Message ID: " + messageId);
                System.out.println("[LocalRedis] SenderId: " + message.getSenderId());
                System.out.println("[LocalRedis] ReceiverId: " + message.getReceiverId());
                System.out.println("[LocalRedis] Encrypted: true");
                System.out.println("[LocalRedis] Content (encrypted): " + (message.getContent() != null ? message.getContent().substring(0, Math.min(30, message.getContent().length())) + "..." : "null"));
                System.out.println("[LocalRedis] IV: " + (message.getIv() != null ? message.getIv().substring(0, Math.min(10, message.getIv().length())) + "..." : "null"));
                System.out.println("[LocalRedis] AuthTag: " + (message.getAuthTag() != null ? message.getAuthTag().substring(0, Math.min(10, message.getAuthTag().length())) + "..." : "null"));
                System.out.println("===============================================================\n");
            }
            
            // ✅ DEBUG: Print stack trace to find who's calling this
            System.out.println("[LocalRedis] Called from:");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (int i = 2; i < Math.min(8, stackTrace.length); i++) {
                System.out.println("    " + stackTrace[i]);
            }
            
            jedis.hset(key, messageId, messageJson);
            System.out.println("[LocalRedis] ✅ Message queued successfully");
            System.out.println("=======================================================\n");
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to queue message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public List<MessageDTO> getAllPendingMessages(Long userId) {
        List<MessageDTO> messages = new ArrayList<>();
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "pending_messages:" + userId;
            Map<String, String> allMessages = jedis.hgetAll(key);
            
            for (String messageJson : allMessages.values()) {
                MessageDTO msg = gson.fromJson(messageJson, MessageDTO.class);
                messages.add(msg);
            }
            
            System.out.println("[LocalRedis] 📂 Retrieved " + messages.size() + " pending messages for user " + userId);
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to get messages: " + e.getMessage());
        }
        
        return messages;
    }
    
    public void removeMessage(Long userId, String messageId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "pending_messages:" + userId;
            jedis.hdel(key, messageId);
            
            System.out.println("[LocalRedis] ✅ Removed message: " + messageId);
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to remove message: " + e.getMessage());
        }
    }
    
    public void clearAllPendingMessages(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "pending_messages:" + userId;
            jedis.del(key);
            
            System.out.println("[LocalRedis] 🗑️ Cleared all pending messages for user " + userId);
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to clear messages: " + e.getMessage());
        }
    }
    
    public int getPendingMessageCount(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "pending_messages:" + userId;
            return Math.toIntExact(jedis.hlen(key));
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to get message count: " + e.getMessage());
            return 0;
        }
    }
    
    // ===== FRIENDS LIST =====
    
    public void saveFriendsList(Long userId, List<UserDTO> friends) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "friends:" + userId;
            
            // Clear existing
            jedis.del(key);
            
            // Add all friends
            for (UserDTO friend : friends) {
                jedis.sadd(key, String.valueOf(friend.getId()));
                
                // Save friend details
                String detailKey = "friend_detail:" + friend.getId();
                Map<String, String> friendData = new HashMap<>();
                friendData.put("id", String.valueOf(friend.getId()));
                friendData.put("username", friend.getUsername());
                if (friend.getAvatarUrl() != null) {
                    friendData.put("avatar", friend.getAvatarUrl());
                }
                
                jedis.hset(detailKey, friendData);
            }
            
            System.out.println("[LocalRedis] 💾 Saved " + friends.size() + " friends for user " + userId);
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to save friends: " + e.getMessage());
        }
    }
    
    public List<UserDTO> getFriendsList(Long userId) {
        List<UserDTO> friends = new ArrayList<>();
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "friends:" + userId;
            Set<String> friendIds = jedis.smembers(key);
            
            for (String friendId : friendIds) {
                String detailKey = "friend_detail:" + friendId;
                Map<String, String> friendData = jedis.hgetAll(detailKey);
                
                if (!friendData.isEmpty()) {
                    UserDTO friend = new UserDTO();
                    friend.setId(Long.parseLong(friendData.get("id")));
                    friend.setUsername(friendData.get("username"));
                    friend.setAvatarUrl(friendData.get("avatar"));
                    friends.add(friend);
                }
            }
            
            System.out.println("[LocalRedis] 📂 Retrieved " + friends.size() + " friends for user " + userId);
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to get friends: " + e.getMessage());
        }
        
        return friends;
    }
    
    // ===== GROUPS LIST =====
    
    public void saveGroupsList(Long userId, List<com.example.api.dto.GroupDTO> groups) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "groups:" + userId;
            
            // Clear existing
            jedis.del(key);
            
            // Add all groups as JSON
            for (com.example.api.dto.GroupDTO group : groups) {
                String groupJson = gson.toJson(group);
                jedis.sadd(key, groupJson);
            }
            
            System.out.println("[LocalRedis] 💾 Saved " + groups.size() + " groups for user " + userId);
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to save groups: " + e.getMessage());
        }
    }
    
    public List<com.example.api.dto.GroupDTO> getGroupsList(Long userId) {
        List<com.example.api.dto.GroupDTO> groups = new ArrayList<>();
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "groups:" + userId;
            Set<String> groupJsons = jedis.smembers(key);
            
            for (String groupJson : groupJsons) {
                com.example.api.dto.GroupDTO group = gson.fromJson(groupJson, com.example.api.dto.GroupDTO.class);
                groups.add(group);
            }
            
            System.out.println("[LocalRedis] 📂 Retrieved " + groups.size() + " groups for user " + userId);
        } catch (Exception e) {
            System.err.println("[LocalRedis] ❌ Failed to get groups: " + e.getMessage());
        }
        
        return groups;
    }
    
    // ===== UTILITY =====
    
    public boolean isConnected() {
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }
    
    public void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
            System.out.println("[LocalRedis] 🔌 Disconnected from local Redis");
        }
    }
}
