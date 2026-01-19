package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.taskhero.common.model.enums.Role;

import java.util.Map;

/**
 * Статистика системы для админ-панели.
 */
@Schema(description = "Статистика системы")
public record SystemStatisticsDto(

        @Schema(description = "Общее количество пользователей")
        long totalUsers,

        @Schema(description = "Общее количество родителей")
        long totalParents,

        @Schema(description = "Общее количество детей")
        long totalChildren,

        @Schema(description = "Количество активных пользователей")
        long activeUsers,

        @Schema(description = "Количество неактивных пользователей")
        long inactiveUsers,

        @Schema(description = "Распределение пользователей по ролям")
        Map<Role, Long> usersByRole
) {}
