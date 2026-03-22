package ru.taskhero.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.taskhero.userservice.entity.AuditAction;
import ru.taskhero.userservice.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

/**
 * Репозиторий для журнала аудита.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> findByAdminId(UUID adminId, Pageable pageable);

    Page<AuditLog> findByTargetId(UUID targetId, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);
}
