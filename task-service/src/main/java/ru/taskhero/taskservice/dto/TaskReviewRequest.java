package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * DTO для проверки задания родителем.
 */
@Schema(description = "Запрос на проверку задания родителем")
public record TaskReviewRequest(

        @Schema(description = "Комментарий родителя", example = "Молодец! Отлично справился!")
        @Size(max = 512, message = "Комментарий не должен превышать 512 символов")
        String comment,

        @Schema(description = "EXP для начисления (если отличается от шаблона)", example = "30")
        @Min(value = 0, message = "Минимальный EXP - 0")
        @Max(value = 1000, message = "Максимальный EXP - 1000")
        Integer expReward,

        @Schema(description = "Коины для начисления (если отличаются от шаблона)", example = "15")
        @Min(value = 0, message = "Минимальные коины - 0")
        @Max(value = 500, message = "Максимальные коины - 500")
        Integer coinsReward
) {
}
