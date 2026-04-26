package ru.taskhero.userservice.dto;

import ru.taskhero.userservice.entity.AuditAction;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO для записи журнала аудита.
 */
public record AuditLogDto(
        UUID id,
        UUID adminId,
        String adminEmail,
        AuditAction action,
        String targetType,
        UUID targetId,
        String details,
        Instant createdAt
) { }
