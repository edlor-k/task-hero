package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO ответа с информацией о товаре магазина.
 */
@Schema(description = "Информация о товаре магазина")
public record ShopItemResponseDto(

        @Schema(description = "ID товара")
        UUID id,

        @Schema(description = "ID родителя")
        UUID parentId,

        @Schema(description = "Название")
        String title,

        @Schema(description = "Описание")
        String description,

        @Schema(description = "Цена в коинах")
        int priceCoins,

        @Schema(description = "Иконка")
        String iconName,

        @Schema(description = "Активен ли товар")
        boolean active,

        @Schema(description = "ID детей, которым доступен товар")
        List<UUID> childIds,

        @Schema(description = "Дата создания")
        Instant createdAt,

        @Schema(description = "Элемент маркетплейса (системный шаблон)")
        boolean marketplaceItem
) {
}
