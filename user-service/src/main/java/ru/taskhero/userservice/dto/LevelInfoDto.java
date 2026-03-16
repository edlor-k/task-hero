package ru.taskhero.userservice.dto;

public record LevelInfoDto(
        int level,
        int expRequired,
        int totalExpForLevel,
        int approxEasyTasks,
        int approxNormalTasks,
        int approxHardTasks
) {
}
