package ru.taskhero.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:dateFrom IS NULL OR a.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR a.createdAt <= :dateTo) " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> findFiltered(
            @Param("action") AuditAction action,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable
    );
}
