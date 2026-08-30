package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

import java.util.UUID;

/**
 * DTO для начисления наград ребёнку.
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
        boolean capExp,

        @Schema(description = "ID назначения задания — источник награды. Если задан, " +
                "повторный запрос с тем же ID не приведёт к повторному начислению (идемпотентность).")
        UUID sourceAssignmentId
) {
}
