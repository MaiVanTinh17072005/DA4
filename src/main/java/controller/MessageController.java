package controller;

import dto.MessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.MessageService;
import service.RedisMessageQueueService;
import util.JwtUtil;

import java.util.List;

/**
 * Message Controller
 * REST API endpoints for message management
 */
@RestController
@RequestMapping("/api/v1/messages")
@CrossOrigin(origins = "*") // Allow requests from JavaFX client
public class MessageController {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private RedisMessageQueueService redisMessageQueueService;
    
    /**
     * Get all messages for current user
     * GET /api/v1/messages
     * 
     * @param authorization JWT token from Authorization header
     * @return List of messages
     */
    @GetMapping
    public ResponseEntity<List<MessageDTO>> getAllMessages(
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== GET ALL MESSAGES ===");
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Get messages
            List<MessageDTO> messages = messageService.getMessagesForUser(userId);
            
            System.out.println("✅ Returning " + messages.size() + " messages");
            System.out.println("========================");
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("========================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get conversation with a specific user
     * GET /api/v1/messages/conversation/{userId}
     * 
     * @param userId Other user's ID
     * @param authorization JWT token from Authorization header
     * @return List of messages in conversation
     */
    @GetMapping("/conversation/{userId}")
    public ResponseEntity<List<MessageDTO>> getConversation(
            @PathVariable("userId") Long userId,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== GET CONVERSATION ===");
        System.out.println("Other User ID: " + userId);
        
        try {
            // Extract current user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long currentUserId = JwtUtil.extractUserId(token);
            
            System.out.println("Current User ID: " + currentUserId);
            
            // Get conversation
            List<MessageDTO> messages = messageService.getConversation(currentUserId, userId);
            
            System.out.println("✅ Returning " + messages.size() + " messages");
            System.out.println("========================");
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("========================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get messages in a group
     * GET /api/v1/messages/group/{groupId}
     * 
     * @param groupId Group ID
     * @param authorization JWT token from Authorization header
     * @return List of messages in group
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<MessageDTO>> getGroupMessages(
            @PathVariable("groupId") Long groupId,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== GET GROUP MESSAGES ===");
        System.out.println("Group ID: " + groupId);
        
        try {
            // Extract user ID from JWT token (for authentication)
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Get group messages
            List<MessageDTO> messages = messageService.getGroupMessages(groupId);
            
            System.out.println("✅ Returning " + messages.size() + " messages");
            System.out.println("==========================");
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("==========================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Send a new message
     * POST /api/v1/messages/send
     * Body: MessageDTO
     * 
     * @param messageDTO Message data
     * @param authorization JWT token from Authorization header
     * @return Sent message
     */
    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestBody MessageDTO messageDTO,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== SEND MESSAGE ===");
        
        try {
            // Extract user ID from JWT token
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            // Set sender ID from token
            messageDTO.setSenderId(userId);
            
            System.out.println("Sender ID: " + userId);
            System.out.println("Message Type: " + messageDTO.getMsgType());
            
            // Send message
            MessageDTO sentMessage = messageService.sendMessage(messageDTO);
            
            if (sentMessage != null) {
                System.out.println("✅ Message sent successfully");
                System.out.println("====================");
                return ResponseEntity.ok(sentMessage);
            } else {
                System.err.println("❌ Failed to send message");
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
     * Queue a message to Redis (used when server is "offline")
     * POST /api/v1/messages/queue
     * Body: MessageDTO
     * 
     * @param messageDTO Message data
     * @return Success response
     */
    @PostMapping("/queue")
    public ResponseEntity<String> queueMessage(@RequestBody MessageDTO messageDTO) {
        
        System.out.println("=== QUEUE MESSAGE TO REDIS ===");
        
        try {
            System.out.println("Sender ID: " + messageDTO.getSenderId());
            System.out.println("Receiver ID: " + messageDTO.getReceiverId());
            System.out.println("Content: " + messageDTO.getContent());
            System.out.println("MsgId: " + messageDTO.getMsgId());
            System.out.println("Encrypted: " + messageDTO.getAesEncrypted());
            
            // CRITICAL: Validate senderId is not null
            if (messageDTO.getSenderId() == null) {
                System.err.println("❌ ERROR: SenderId is NULL! Cannot queue message.");
                System.err.println("Full MessageDTO: " + messageDTO);
                return ResponseEntity.badRequest()
                    .body("Failed to queue message: senderId is required");
            }
            
            // Queue message to Redis
            redisMessageQueueService.queueMessage(messageDTO);
            
            System.out.println("✅ Message queued to Redis successfully");
            System.out.println("==============================");
            return ResponseEntity.ok("Message queued successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error queuing message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("==============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to queue message: " + e.getMessage());
        }
    }
    
    /**
     * Mark message as read
     * PUT /api/v1/messages/{msgId}/read
     * 
     * @param msgId Message ID
     * @param authorization JWT token from Authorization header
     * @return Success response
     */
    @PutMapping("/{msgId}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable("msgId") Long msgId,
            @RequestHeader("Authorization") String authorization) {
        
        System.out.println("=== MARK MESSAGE AS READ ===");
        System.out.println("Message ID: " + msgId);
        
        try {
            // Extract user ID from JWT token (for authentication)
            String token = authorization.replace("Bearer ", "");
            Long userId = JwtUtil.extractUserId(token);
            
            System.out.println("User ID: " + userId);
            
            // Mark as read
            boolean success = messageService.markAsRead(msgId);
            
            if (success) {
                System.out.println("✅ Message marked as read");
                System.out.println("============================");
                return ResponseEntity.ok("Message marked as read");
            } else {
                System.err.println("❌ Failed to mark message as read");
                System.out.println("============================");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to mark message as read");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("============================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
}
