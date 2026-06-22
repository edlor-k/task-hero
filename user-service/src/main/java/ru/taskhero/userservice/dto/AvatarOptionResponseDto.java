package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO опции аватара из галереи.
 */
@Schema(description = "Опция аватара из галереи")
public record AvatarOptionResponseDto(
        @Schema(description = "ID опции")
        UUID id,

        @Schema(description = "Публичный URL картинки")
        String imageUrl,

        @Schema(description = "Отображаемое название")
        String label,

        @Schema(description = "Порядок отображения")
        int sortOrder,

        @Schema(description = "Активна ли опция (видна ли ребёнку в пикере)")
        boolean active
) {
}
