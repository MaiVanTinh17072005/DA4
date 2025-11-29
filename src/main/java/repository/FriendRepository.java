package repository;

import model.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Friend entity
 */
@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    
    /**
     * Find all friends for a user
     */
    List<Friend> findByUserId(Long userId);
    
    /**
     * Find all friends where user is target
     */
    List<Friend> findByTargetId(Long targetId);
    
    /**
     * Find friend relationship between two users
     */
    Optional<Friend> findByUserIdAndTargetId(Long userId, Long targetId);
    
    /**
     * Find all friends with specific status
     */
    List<Friend> findByUserIdAndStatus(Long userId, String status);
}
