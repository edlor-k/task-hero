package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.taskhero.common.model.enums.RepeatUnit;
import ru.taskhero.common.model.enums.TaskCategory;
import ru.taskhero.common.model.enums.TaskDifficulty;

import java.util.List;

/**
 * DTO для создания шаблона задания.
 */
@Schema(description = "Запрос на создание шаблона задания")
public record TaskTemplateCreateRequest(

        @Schema(description = "Название задания", example = "Убраться в комнате")
        @NotBlank(message = "Название задания обязательно")
        @Size(min = 3, max = 128, message = "Название должно быть от 3 до 128 символов")
        String title,

        @Schema(description = "Описание задания", example = "Навести порядок на столе, убрать игрушки, пропылесосить")
        @Size(max = 1024, message = "Описание не должно превышать 1024 символа")
        String description,

        @Schema(description = "Награда в EXP", example = "25", minimum = "1", maximum = "1000")
        @Min(value = 1, message = "Минимальная награда EXP - 1")
        @Max(value = 1000, message = "Максимальная награда EXP - 1000")
        Integer expReward,

        @Schema(description = "Награда в коинах", example = "10", minimum = "0", maximum = "500")
        @Min(value = 0, message = "Минимальная награда коинов - 0")
        @Max(value = 500, message = "Максимальная награда коинов - 500")
        Integer coinsReward,

        @Schema(description = "Повторяемое ли задание", example = "true")
        Boolean repeatable,

        @Schema(description = "Расписание повторения", example = "DAILY")
        @Size(max = 64, message = "Расписание не должно превышать 64 символа")
        String repeatSchedule,

        @Schema(description = "Количество повторений", example = "5", minimum = "1", maximum = "100")
        @Min(value = 1, message = "Минимальное количество повторений - 1")
        @Max(value = 100, message = "Максимальное количество повторений - 100")
        Integer repeatCount,

        @Schema(description = "Интервал повторения", example = "1", minimum = "1", maximum = "365")
        @Min(value = 1, message = "Минимальный интервал - 1")
        @Max(value = 365, message = "Максимальный интервал - 365")
        Integer repeatInterval,

        @Schema(description = "Единица интервала повторения", example = "WEEKS")
        RepeatUnit repeatUnit,

        @Schema(description = "Уровень сложности задания", example = "NORMAL")
        TaskDifficulty difficulty,

        @Schema(description = "Правило повторения в формате RRULE",
                example = "FREQ=WEEKLY;BYDAY=MO,WE,FR")
        @Size(max = 256, message = "Правило повторения не должно превышать 256 символов")
        String recurrenceRule,

        @Schema(description = "Категория задания", example = "STUDY")
        TaskCategory category,

        @Schema(description = "Подпункты задания (для составных заданий)")
        List<String> subItems
) {
}
