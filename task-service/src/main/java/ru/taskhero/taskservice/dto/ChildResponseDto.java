package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO ответа с информацией о ребёнке.
 * Используется для получения данных от user-service через Feign.
 */
@Schema(description = "Информация о ребёнке")
public record ChildResponseDto(

        @Schema(description = "ID ребёнка")
        UUID id,

        @Schema(description = "Имя ребёнка")
        String firstName,

        @Schema(description = "Фамилия ребёнка")
        String surname,

        @Schema(description = "Токен для входа")
        String loginToken,

        @Schema(description = "Текущий EXP")
        int exp,

        @Schema(description = "Текущие коины")
        int coins,

        @Schema(description = "Текущий уровень")
        int level,

        @Schema(description = "URL аватара")
        String avatarUrl,

        @Schema(description = "Дата создания")
        Instant createdAt,

        @Schema(description = "Дата последнего обновления")
        Instant updatedAt
) {
}
