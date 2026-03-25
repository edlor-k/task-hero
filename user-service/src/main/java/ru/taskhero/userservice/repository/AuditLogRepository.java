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

    @Query(value = "SELECT * FROM audit_log a WHERE " +
            "(CAST(:action AS VARCHAR) IS NULL OR a.action = CAST(:action AS VARCHAR)) AND " +
            "(CAST(:dateFrom AS TIMESTAMP) IS NULL OR a.created_at >= CAST(:dateFrom AS TIMESTAMP)) AND " +
            "(CAST(:dateTo AS TIMESTAMP) IS NULL OR a.created_at <= CAST(:dateTo AS TIMESTAMP)) " +
            "ORDER BY a.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM audit_log a WHERE " +
            "(CAST(:action AS VARCHAR) IS NULL OR a.action = CAST(:action AS VARCHAR)) AND " +
            "(CAST(:dateFrom AS TIMESTAMP) IS NULL OR a.created_at >= CAST(:dateFrom AS TIMESTAMP)) AND " +
            "(CAST(:dateTo AS TIMESTAMP) IS NULL OR a.created_at <= CAST(:dateTo AS TIMESTAMP))",
            nativeQuery = true)
    Page<AuditLog> findFiltered(
            @Param("action") String action,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable
    );
}
