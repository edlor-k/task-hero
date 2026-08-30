package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO для атомарного назначения задания: либо по существующему шаблону
 * ({@code templateId} задан), либо с одновременным созданием нового шаблона
 * из полей ниже ({@code templateId} не задан). Обе ветки выполняются в
 * одной транзакции на стороне task-service — либо сохраняется и шаблон,
 * и назначение, либо не сохраняется ничего.
 */
@Schema(description = "Запрос на назначение задания (с опциональным одновременным созданием шаблона)")
public record AssignTaskRequest(

        @Schema(description = "ID существующего шаблона. Если не задан — шаблон создаётся из полей ниже")
        UUID templateId,

        @Schema(description = "ID ребёнка")
        @NotNull(message = "ID ребёнка обязателен")
        UUID childId,

        @Schema(description = "Срок выполнения")
        @FutureOrPresent(message = "Срок выполнения должен быть сегодня или в будущем")
        Instant dueDate,

        @Schema(description = "Важное задание")
        Boolean important,

        @Schema(description = "Название нового шаблона (обязательно, если templateId не задан)")
        @Size(min = 3, max = 128, message = "Название должно быть от 3 до 128 символов")
        String title,

        @Schema(description = "Описание нового шаблона")
        @Size(max = 1024, message = "Описание не должно превышать 1024 символа")
        String description,

        @Schema(description = "Награда в коинах для нового шаблона")
        @Min(value = 0, message = "Минимальная награда коинов - 0")
        @Max(value = 500, message = "Максимальная награда коинов - 500")
        Integer coinsReward,

        @Schema(description = "Правило повторения в формате RRULE для нового шаблона")
        @Size(max = 256, message = "Правило повторения не должно превышать 256 символов")
        String recurrenceRule
) {
}
