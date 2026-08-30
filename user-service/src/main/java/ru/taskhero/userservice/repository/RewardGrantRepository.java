package ru.taskhero.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.taskhero.userservice.entity.RewardGrant;

import java.util.UUID;

/**
 * Репозиторий леджера применённых начислений наград.
 */
@Repository
public interface RewardGrantRepository extends JpaRepository<RewardGrant, UUID> {

    boolean existsBySourceAssignmentId(UUID sourceAssignmentId);
}
