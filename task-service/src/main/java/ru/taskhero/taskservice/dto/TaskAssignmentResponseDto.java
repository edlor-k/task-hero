package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.taskhero.common.model.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO ответа с информацией о назначенном задании.
 */
@Schema(description = "Информация о назначенном задании")
public record TaskAssignmentResponseDto(

        @Schema(description = "ID назначения")
        UUID id,

        @Schema(description = "ID ребёнка")
        UUID childId,

        @Schema(description = "Статус задания")
        TaskStatus status,

        @Schema(description = "Срок выполнения")
        LocalDate dueDate,

        @Schema(description = "Комментарий ребёнка при сдаче")
        String childComment,

        @Schema(description = "Комментарий родителя при проверке")
        String parentComment,

        @Schema(description = "Время сдачи задания")
        Instant submittedAt,

        @Schema(description = "Время проверки задания")
        Instant reviewedAt,

        @Schema(description = "Начисленный EXP")
        Integer expEarned,

        @Schema(description = "Начисленные коины")
        Integer coinsEarned,

        @Schema(description = "Важное задание")
        Boolean important,

        @Schema(description = "Информация о шаблоне задания")
        TaskTemplateResponseDto template,

        @Schema(description = "Дата создания")
        Instant createdAt,

        @Schema(description = "Дата последнего обновления")
        Instant updatedAt
) {
}
