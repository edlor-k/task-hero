package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.taskhero.common.model.enums.RepeatUnit;
import ru.taskhero.common.model.enums.TaskCategory;
import ru.taskhero.common.model.enums.TaskDifficulty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO ответа с информацией о шаблоне задания.
 */
@Schema(description = "Информация о шаблоне задания")
public record TaskTemplateResponseDto(

        @Schema(description = "ID шаблона")
        UUID id,

        @Schema(description = "ID родителя-создателя")
        UUID parentId,

        @Schema(description = "Название задания")
        String title,

        @Schema(description = "Описание задания")
        String description,

        @Schema(description = "Награда в EXP")
        int expReward,

        @Schema(description = "Награда в коинах")
        int coinsReward,

        @Schema(description = "Повторяемое ли задание")
        boolean repeatable,

        @Schema(description = "Расписание повторения")
        String repeatSchedule,

        @Schema(description = "Количество повторений")
        Integer repeatCount,

        @Schema(description = "Интервал повторения")
        Integer repeatInterval,

        @Schema(description = "Единица интервала повторения")
        RepeatUnit repeatUnit,

        @Schema(description = "Правило повторения RRULE")
        String recurrenceRule,

        @Schema(description = "Категория задания")
        TaskCategory category,

        @Schema(description = "Уровень сложности")
        TaskDifficulty difficulty,

        @Schema(description = "Активен ли шаблон")
        boolean active,

        @Schema(description = "Является ли библиотечным шаблоном")
        boolean libraryTemplate,

        @Schema(description = "ID исходного шаблона библиотеки")
        UUID sourceTemplateId,

        @Schema(description = "Подпункты задания")
        List<TaskSubItemDto> subItems,

        @Schema(description = "Дата создания")
        Instant createdAt,

        @Schema(description = "Дата последнего обновления")
        Instant updatedAt
) {
}
