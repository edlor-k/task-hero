package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Запрос на обновление данных ребенка.
 */
@Schema(description = "Запрос на обновление данных ребенка")
public record UpdateChildRequest(

        @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
        @Schema(description = "Имя ребенка", example = "Маша")
        String firstName,


        @Min(value = 0, message = "Опыт не может быть отрицательным")
        @Schema(description = "Опыт ребенка", example = "150")
        Integer exp,

        @Min(value = 0, message = "Монеты не могут быть отрицательными")
        @Schema(description = "Количество монет", example = "50")
        Integer coins,

        @Min(value = 1, message = "Минимальный уровень 1")
        @Schema(description = "Уровень ребенка", example = "3")
        Integer level
) {}
