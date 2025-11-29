package repository;

import model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Message entity
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Find all messages sent by a user
     */
    List<Message> findBySenderId(Long senderId);
    
    /**
     * Find all messages received by a user (direct messages)
     */
    List<Message> findByReceiverId(Long receiverId);
    
    /**
     * Find all messages in a group
     */
    List<Message> findByGroupId(Long groupId);
    
    /**
     * Find conversation between two users
     */
    List<Message> findBySenderIdAndReceiverIdOrReceiverIdAndSenderId(
            Long senderId1, Long receiverId1, Long senderId2, Long receiverId2);
    
    /**
     * Find unread messages for a user
     */
    List<Message> findByReceiverIdAndIsReadFalse(Long receiverId);
}
