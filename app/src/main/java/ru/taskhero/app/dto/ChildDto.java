package ru.taskhero.app.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO информации о ребёнке.
 */
public record ChildDto(
        UUID id,
        String firstName,
        String surname,
        String loginToken,
        int exp,
        int coins,
        int level,
        String avatarUrl,
        String difficultyTrajectory,
        String characterType,
        boolean characterSelected,
        String characterImagePath,
        int expToNextLevel,
        int currentLevelExp,
        int nextLevelExp,
        Instant createdAt,
        Instant updatedAt,
        String nickname
) {
}
