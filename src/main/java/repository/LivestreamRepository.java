package repository;

import model.Livestream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Livestream entity
 */
@Repository
public interface LivestreamRepository extends JpaRepository<Livestream, Long> {
    
    /**
     * Find all livestreams by host
     */
    List<Livestream> findByHostId(Long hostId);
    
    /**
     * Find all active livestreams
     */
    List<Livestream> findByStatus(String status);
    
    /**
     * Find active livestreams by host
     */
    List<Livestream> findByHostIdAndStatus(Long hostId, String status);
}
