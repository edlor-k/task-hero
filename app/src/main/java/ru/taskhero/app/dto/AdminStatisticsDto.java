package ru.taskhero.app.dto;

import java.util.Map;

/**
 * DTO статистики системы.
 */
public record AdminStatisticsDto(
        long totalUsers,
        long totalParents,
        long totalChildren,
        long activeUsers,
        long inactiveUsers,
        Map<String, Long> usersByRole
) { }
