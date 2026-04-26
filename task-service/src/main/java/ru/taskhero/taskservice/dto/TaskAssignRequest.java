package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO для назначения задания ребёнку.
 */
@Schema(description = "Запрос на назначение задания ребёнку")
public record TaskAssignRequest(

        @Schema(description = "ID шаблона задания")
        @NotNull(message = "ID шаблона обязателен")
        UUID templateId,

        @Schema(description = "ID ребёнка")
        @NotNull(message = "ID ребёнка обязателен")
        UUID childId,

        @Schema(description = "Срок выполнения", example = "2025-02-01T18:00")
        @FutureOrPresent(message = "Срок выполнения должен быть сегодня или в будущем")
        Instant dueDate,

        @Schema(description = "Важное задание")
        Boolean important
) {
}
