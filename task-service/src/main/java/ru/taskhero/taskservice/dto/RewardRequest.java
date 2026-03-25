package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/**
 * DTO для начисления наград ребёнку.
 * Используется при взаимодействии с user-service.
 */
@Schema(description = "Запрос на начисление наград")
public record RewardRequest(

        @Schema(description = "EXP для начисления", example = "25")
        @Min(value = 0, message = "EXP не может быть отрицательным")
        int exp,

        @Schema(description = "Коины для начисления", example = "10")
        @Min(value = 0, message = "Коины не могут быть отрицательными")
        int coins,

        @Schema(description = "Ограничить EXP максимумом текущего уровня")
        boolean capExp
) {
}
