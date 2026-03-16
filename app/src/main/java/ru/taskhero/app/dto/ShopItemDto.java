package ru.taskhero.app.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO информации о товаре магазина.
 */
public record ShopItemDto(
        UUID id,
        UUID parentId,
        String title,
        String description,
        int priceCoins,
        String iconName,
        boolean active,
        List<UUID> childIds,
        Instant createdAt,
        boolean marketplaceItem
) {
}
