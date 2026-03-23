package ru.taskhero.userservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.taskhero.userservice.dto.AuditLogDto;
import ru.taskhero.userservice.entity.AuditAction;

import java.time.Instant;
import java.util.UUID;

/**
 * Сервис журнала аудита.
 */
public interface AuditService {

    /**
     * Записать действие в журнал.
     */
    void log(UUID adminId, String adminEmail, AuditAction action,
             String targetType, UUID targetId, String details);

    /**
     * Получить записи аудита с пагинацией.
     */
    Page<AuditLogDto> getAuditLogs(Pageable pageable);

    /**
     * Получить записи аудита с фильтрацией.
     */
    Page<AuditLogDto> getAuditLogs(Pageable pageable, AuditAction action,
                                    Instant dateFrom, Instant dateTo);

    /**
     * Получить последние N записей.
     */
    Page<AuditLogDto> getRecentLogs(int count);
}
