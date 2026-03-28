package ru.taskhero.app.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO информации о назначенном задании.
 */
public record TaskAssignmentDto(
        UUID id,
        UUID childId,
        String status,
        LocalDateTime dueDate,
        String childComment,
        String parentComment,
        Instant submittedAt,
        Instant reviewedAt,
        Integer expEarned,
        Integer coinsEarned,
        Boolean important,
        Boolean reassigned,
        TaskTemplateDto template,
        Instant createdAt,
        Instant updatedAt
) {
}
