package ru.taskhero.app.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO информации о шаблоне задания.
 */
public record TaskTemplateDto(
        UUID id,
        UUID parentId,
        String title,
        String description,
        int expReward,
        int coinsReward,
        boolean repeatable,
        String repeatSchedule,
        Integer repeatCount,
        Integer repeatInterval,
        String repeatUnit,
        String difficulty,
        boolean active,
        String recurrenceRule,
        String category,
        boolean libraryTemplate,
        UUID sourceTemplateId,
        List<TaskSubItemDto> subItems,
        String presetGroup,
        Instant createdAt,
        Instant updatedAt
) {
}
