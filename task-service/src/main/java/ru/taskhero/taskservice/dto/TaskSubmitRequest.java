package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * DTO для сдачи задания ребёнком.
 */
@Schema(description = "Запрос на сдачу задания ребёнком")
public record TaskSubmitRequest(

        @Schema(description = "Комментарий ребёнка", example = "Я все сделал! Можешь проверить.")
        @Size(max = 512, message = "Комментарий не должен превышать 512 символов")
        String comment
) {
}
