package ru.taskhero.app.dto;

/**
 * DTO информации об уровне (XP и примерное количество заданий).
 */
public record LevelInfoDto(
        int level,
        int expRequired,
        int totalExpForLevel,
        int approxEasyTasks,
        int approxNormalTasks,
        int approxHardTasks
) {
}
