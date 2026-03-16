package ru.taskhero.app.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO информации о покупке.
 */
public record ShopPurchaseDto(
        UUID id,
        UUID childId,
        String childFirstName,
        ShopItemDto shopItem,
        String status,
        String parentComment,
        Instant createdAt,
        Instant reviewedAt
) {
}
