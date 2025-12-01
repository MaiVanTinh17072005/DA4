package repository;

import model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Group entity
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    /**
     * Find all groups owned by a user
     */
    List<Group> findByOwnerId(Long ownerId);
    
    /**
     * Find groups by name (case-insensitive search)
     */
    List<Group> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find all groups where user is a member
     * Joins with group_member table
     */
    @Query("SELECT g FROM Group g JOIN GroupMember gm ON g.groupId = gm.groupId WHERE gm.userId = :userId")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);
}
