package repository;

import model.UserKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for UserKey entity
 */
@Repository
public interface UserKeyRepository extends JpaRepository<UserKey, Long> {
}
