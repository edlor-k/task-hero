package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.taskhero.common.model.enums.PurchaseStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO ответа с информацией о покупке.
 */
@Schema(description = "Информация о покупке")
public record ShopPurchaseResponseDto(

        @Schema(description = "ID покупки")
        UUID id,

        @Schema(description = "ID ребёнка")
        UUID childId,

        @Schema(description = "Имя ребёнка")
        String childFirstName,

        @Schema(description = "Информация о товаре")
        ShopItemResponseDto shopItem,

        @Schema(description = "Статус покупки")
        PurchaseStatus status,

        @Schema(description = "Комментарий родителя")
        String parentComment,

        @Schema(description = "Дата запроса")
        Instant createdAt,

        @Schema(description = "Дата проверки")
        Instant reviewedAt
) {
}
