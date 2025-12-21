package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.MessageDTO;
import model.Message;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.MessageRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Message Service
 * Handles message-related business logic with Redis caching
 */
@Service
public class MessageService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisService redisService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    // Redis key patterns
    private static final String USER_MESSAGES_PREFIX = "user:messages:";
    private static final String CONVERSATION_PREFIX = "conversation:";
    private static final String GROUP_MESSAGES_PREFIX = "group:messages:";
    
    // TTL: 3 days in seconds
    private static final int MESSAGES_CACHE_TTL = 3 * 24 * 60 * 60; // 259200 seconds
    
    /**
     * Get all messages for a user (both sent and received)
     * Cached in Redis
     */
    public List<MessageDTO> getMessagesForUser(Long userId) {
        System.out.println("[MessageService] Getting messages for user: " + userId);
        
        try {
            // Try to get from Redis cache first
            String cacheKey = USER_MESSAGES_PREFIX + userId;
            List<MessageDTO> cachedMessages = redisService.getCachedList(cacheKey, MessageDTO.class);
            
            if (cachedMessages != null) {
                System.out.println("[MessageService] ✅ Found " + cachedMessages.size() + " messages in Redis cache");
                return cachedMessages;
            }
            
            // Cache miss - get from DB
            System.out.println("[MessageService] ⚠️ Cache miss - fetching from DB");
            List<Message> messages = messageRepository.findBySenderIdOrReceiverId(userId, userId);
            
            // Convert to DTOs
            List<MessageDTO> messageDTOs = messages.stream()
                    .map(this::convertToDTO)
                    .sorted(Comparator.comparing(MessageDTO::getTimestamp).reversed())
                    .collect(Collectors.toList());
            
            // Update cache
            redisService.cacheObject(cacheKey, messageDTOs, MESSAGES_CACHE_TTL);
            
            System.out.println("[MessageService] ✅ Returning " + messageDTOs.size() + " messages from DB");
            return messageDTOs;
            
        } catch (Exception e) {
            System.err.println("[MessageService] ❌ Error getting messages: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Get conversation between two users
     * Cached in Redis
     */
    public List<MessageDTO> getConversation(Long userId1, Long userId2) {
        System.out.println("[MessageService] Getting conversation between " + userId1 + " and " + userId2);
        
        try {
            // Create cache key with sorted IDs to ensure consistency
            Long minId = Math.min(userId1, userId2);
            Long maxId = Math.max(userId1, userId2);
            String cacheKey = CONVERSATION_PREFIX + minId + ":" + maxId;
            
            // Try to get from Redis cache first
            List<MessageDTO> cachedMessages = redisService.getCachedList(cacheKey, MessageDTO.class);
            
            if (cachedMessages != null) {
                System.out.println("[MessageService] ✅ Found " + cachedMessages.size() + " messages in Redis cache");
                return cachedMessages;
            }
            
            // Cache miss - get from DB
            System.out.println("[MessageService] ⚠️ Cache miss - fetching from DB");
            List<Message> messages = messageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderId(
                    userId1, userId2, userId1, userId2);
            
            // Convert to DTOs
            List<MessageDTO> messageDTOs = messages.stream()
                    .map(this::convertToDTO)
                    .sorted(Comparator.comparing(MessageDTO::getTimestamp))
                    .collect(Collectors.toList());
            
            // Update cache
            redisService.cacheObject(cacheKey, messageDTOs, MESSAGES_CACHE_TTL);
            
            System.out.println("[MessageService] ✅ Returning " + messageDTOs.size() + " messages from DB");
            return messageDTOs;
            
        } catch (Exception e) {
            System.err.println("[MessageService] ❌ Error getting conversation: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Get messages in a group
     * Cached in Redis
     */
    public List<MessageDTO> getGroupMessages(Long groupId) {
        System.out.println("[MessageService] Getting messages for group: " + groupId);
        
        try {
            // Try to get from Redis cache first
            String cacheKey = GROUP_MESSAGES_PREFIX + groupId;
            List<MessageDTO> cachedMessages = redisService.getCachedList(cacheKey, MessageDTO.class);
            
            if (cachedMessages != null) {
                System.out.println("[MessageService] ✅ Found " + cachedMessages.size() + " messages in Redis cache");
                return cachedMessages;
            }
            
            // Cache miss - get from DB
            System.out.println("[MessageService] ⚠️ Cache miss - fetching from DB");
            List<Message> messages = messageRepository.findByGroupId(groupId);
            
            // Convert to DTOs
            List<MessageDTO> messageDTOs = messages.stream()
                    .map(this::convertToDTO)
                    .sorted(Comparator.comparing(MessageDTO::getTimestamp))
                    .collect(Collectors.toList());
            
            // Update cache
            redisService.cacheObject(cacheKey, messageDTOs, MESSAGES_CACHE_TTL);
            
            System.out.println("[MessageService] ✅ Returning " + messageDTOs.size() + " messages from DB");
            return messageDTOs;
            
        } catch (Exception e) {
            System.err.println("[MessageService] ❌ Error getting group messages: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Send a new message
     * Invalidates relevant caches
     */
    public MessageDTO sendMessage(MessageDTO messageDTO) {
        System.out.println("[MessageService] Sending message from " + messageDTO.getSenderId());
        System.out.println("[MessageService] Encrypted: " + messageDTO.getAesEncrypted());
        
        try {
            // Create Message entity
            Message message = new Message();
            message.setSenderId(messageDTO.getSenderId());
            message.setReceiverId(messageDTO.getReceiverId());
            message.setGroupId(messageDTO.getGroupId());
            message.setContent(messageDTO.getContent());
            message.setMsgType(messageDTO.getMsgType());
            message.setFilePath(messageDTO.getFilePath());
            message.setTimestamp(LocalDateTime.now());
            message.setIsRead(false);
            message.setAesEncrypted(messageDTO.getAesEncrypted() != null ? messageDTO.getAesEncrypted() : false);
            
            // IMPORTANT: Save E2EE fields if message is encrypted
            if (messageDTO.getAesEncrypted() != null && messageDTO.getAesEncrypted()) {
                message.setIv(messageDTO.getIv());
                message.setAuthTag(messageDTO.getAuthTag());
                message.setAlgorithm(messageDTO.getAlgorithm());
                System.out.println("[MessageService] 🔒 Saving E2EE fields:");
                System.out.println("  - IV: " + (messageDTO.getIv() != null ? messageDTO.getIv().substring(0, Math.min(20, messageDTO.getIv().length())) + "..." : "null"));
                System.out.println("  - AuthTag: " + (messageDTO.getAuthTag() != null ? messageDTO.getAuthTag().substring(0, Math.min(20, messageDTO.getAuthTag().length())) + "..." : "null"));
                System.out.println("  - Algorithm: " + messageDTO.getAlgorithm());
            }
            
            // Save to DB
            Message savedMessage = messageRepository.save(message);
            
            // Invalidate caches
            invalidateMessageCaches(messageDTO.getSenderId(), messageDTO.getReceiverId(), messageDTO.getGroupId());
            
            System.out.println("[MessageService] ✅ Message sent successfully: ID=" + savedMessage.getMsgId());
            
            // Return DTO
            return convertToDTO(savedMessage);
            
        } catch (Exception e) {
            System.err.println("[MessageService] ❌ Error sending message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Mark message as read
     * Updates cache
     */
    public boolean markAsRead(Long msgId) {
        System.out.println("[MessageService] Marking message as read: " + msgId);
        
        try {
            Optional<Message> messageOpt = messageRepository.findById(msgId);
            
            if (messageOpt.isEmpty()) {
                System.err.println("[MessageService] ❌ Message not found: " + msgId);
                return false;
            }
            
            Message message = messageOpt.get();
            message.setIsRead(true);
            messageRepository.save(message);
            
            // Invalidate caches
            invalidateMessageCaches(message.getSenderId(), message.getReceiverId(), message.getGroupId());
            
            System.out.println("[MessageService] ✅ Message marked as read");
            return true;
            
        } catch (Exception e) {
            System.err.println("[MessageService] ❌ Error marking message as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Invalidate message caches when data changes
     */
    private void invalidateMessageCaches(Long senderId, Long receiverId, Long groupId) {
        try {
            // Invalidate sender's messages cache
            if (senderId != null) {
                redisService.removeCachedObject(USER_MESSAGES_PREFIX + senderId);
            }
            
            // Invalidate receiver's messages cache
            if (receiverId != null) {
                redisService.removeCachedObject(USER_MESSAGES_PREFIX + receiverId);
                
                // Invalidate conversation cache
                if (senderId != null) {
                    Long minId = Math.min(senderId, receiverId);
                    Long maxId = Math.max(senderId, receiverId);
                    redisService.removeCachedObject(CONVERSATION_PREFIX + minId + ":" + maxId);
                }
            }
            
            // Invalidate group messages cache
            if (groupId != null) {
                redisService.removeCachedObject(GROUP_MESSAGES_PREFIX + groupId);
            }
            
            System.out.println("[MessageService] ✅ Message caches invalidated");
            
        } catch (Exception e) {
            System.err.println("[MessageService] ❌ Error invalidating caches: " + e.getMessage());
        }
    }
    
    /**
     * Convert Message entity to MessageDTO
     */
    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setMsgId(message.getMsgId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setGroupId(message.getGroupId());
        dto.setContent(message.getContent());
        dto.setMsgType(message.getMsgType());
        dto.setFilePath(message.getFilePath());
        dto.setTimestamp(message.getTimestamp().format(DATE_FORMATTER));
        dto.setIsRead(message.getIsRead());
        dto.setAesEncrypted(message.getAesEncrypted());
        
        // IMPORTANT: Include E2EE fields if message is encrypted
        if (message.getAesEncrypted() != null && message.getAesEncrypted()) {
            dto.setIv(message.getIv());
            dto.setAuthTag(message.getAuthTag());
            dto.setAlgorithm(message.getAlgorithm());
        }
        
        // Get sender username
        Optional<User> senderOpt = userRepository.findById(message.getSenderId());
        if (senderOpt.isPresent()) {
            dto.setSenderUsername(senderOpt.get().getUsername());
        }
        
        return dto;
    }
}
