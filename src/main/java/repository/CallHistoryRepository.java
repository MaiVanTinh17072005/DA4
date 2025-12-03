package repository;

import model.CallHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for CallHistory entity
 */
@Repository
public interface CallHistoryRepository extends JpaRepository<CallHistory, Long> {
    
    /**
     * Find all calls made by a user
     */
    List<CallHistory> findByCallerId(Long callerId);
    
    /**
     * Find all calls received by a user
     */
    List<CallHistory> findByReceiverId(Long receiverId);
    
    /**
     * Find all calls for a user (both as caller and receiver)
     */
    List<CallHistory> findByCallerIdOrReceiverId(Long callerId, Long receiverId);
    
    /**
     * Find successful calls only
     */
    List<CallHistory> findByCallerIdAndSuccessTrue(Long callerId);
    
    /**
     * Find calls by type (voice, video, group)
     */
    List<CallHistory> findByCallerIdAndType(Long callerId, String type);
}
