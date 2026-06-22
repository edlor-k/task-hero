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
        UUID avatarOptionId,
        int expToNextLevel,
        int currentLevelExp,
        int nextLevelExp,
        Instant createdAt,
        Instant updatedAt,
        String nickname,
        boolean nicknameUnlocked,
        String backgroundUrl,
        UUID backgroundOptionId,
        String accentColor
) {
    public boolean avatarSelected() {
        return avatarOptionId != null;
    }

    public boolean backgroundSelected() {
        return backgroundOptionId != null;
    }

    public String effectiveAccentColor() {
        return accentColor != null ? accentColor : "primary";
    }
}
