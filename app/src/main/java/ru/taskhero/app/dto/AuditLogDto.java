package ru.taskhero.app.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO записи журнала аудита.
 */
public record AuditLogDto(
        UUID id,
        UUID adminId,
        String adminEmail,
        String action,
        String targetType,
        UUID targetId,
        String details,
        Instant createdAt
) { }
