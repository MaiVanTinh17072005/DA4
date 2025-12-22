package repository;

import model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GroupMember entity
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    /**
     * Find all members of a group
     */
    List<GroupMember> findByGroupId(Long groupId);
    
    /**
     * Find all groups a user is member of
     */
    List<GroupMember> findByUserId(Long userId);
    
    /**
     * Check if user is member of a group
     */
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    
    /**
     * Find specific membership record
     */
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    
    /**
     * Delete membership record
     */
    void deleteByGroupIdAndUserId(Long groupId, Long userId);
    
    /**
     * Delete all members of a group (when group is deleted)
     */
    void deleteByGroupId(Long groupId);
    
    /**
     * Count members in a group
     */
    long countByGroupId(Long groupId);
}
