package ru.taskhero.userservice.dto;

import java.time.Instant;
import java.util.UUID;

public record LevelRewardResponseDto(
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
