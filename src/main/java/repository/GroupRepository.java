package repository;

import model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
