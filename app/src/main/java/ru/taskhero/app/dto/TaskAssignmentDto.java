package ru.taskhero.app.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO информации о назначенном задании.
 */
public record TaskAssignmentDto(
        UUID id,
        UUID childId,
        String status,
        LocalDate dueDate,
        String childComment,
        String parentComment,
        Instant submittedAt,
        Instant reviewedAt,
        Integer expEarned,
        Integer coinsEarned,
        Boolean important,
        TaskTemplateDto template,
        Instant createdAt,
        Instant updatedAt
) {
}
