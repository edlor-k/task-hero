package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO для создания товара в магазине.
 */
@Schema(description = "Запрос на создание товара")
public record ShopItemCreateRequest(

        @Schema(description = "Название товара", example = "Поход в кино")
        @NotBlank(message = "Название товара обязательно")
        @Size(min = 2, max = 128, message = "Название должно быть от 2 до 128 символов")
        String title,

        @Schema(description = "Описание товара", example = "Семейный поход в кинотеатр на любой фильм")
        @Size(max = 512, message = "Описание не должно превышать 512 символов")
        String description,

        @Schema(description = "Цена в коинах", example = "50", minimum = "1")
        @Min(value = 1, message = "Минимальная цена - 1 коин")
        int priceCoins,

        @Schema(description = "Иконка товара (Bootstrap Icons)", example = "bi-film")
        @Size(max = 64)
        String iconName,

        @Schema(description = "Список ID детей, которым доступен товар")
        @NotEmpty(message = "Нужно указать хотя бы одного ребёнка")
        List<UUID> childIds
) {
}
