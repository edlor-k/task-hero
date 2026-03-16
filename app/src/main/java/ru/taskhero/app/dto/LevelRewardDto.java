package ru.taskhero.app.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO награды за достижение уровня.
 */
public record LevelRewardDto(
        UUID id,
        UUID childId,
        int level,
        String title,
        String description,
        UUID shopItemId,
        String shopItemTitle,
        boolean claimed,
        Instant claimedAt,
        boolean issued,
        Instant issuedAt,
        boolean seen
) {
}
