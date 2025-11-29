package repository;

import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 * Provides database operations for User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email
     * 
     * @param email User email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by username
     * 
     * @param username Username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Check if email exists
     * 
     * @param email Email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if username exists
     * 
     * @param username Username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);
}
