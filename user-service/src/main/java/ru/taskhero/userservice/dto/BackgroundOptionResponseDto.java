package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Опция фона из галереи")
public record BackgroundOptionResponseDto(
        @Schema(description = "ID опции")
        UUID id,

        @Schema(description = "Публичный URL картинки")
        String imageUrl,

        @Schema(description = "Отображаемое название")
        String label,

        @Schema(description = "Порядок отображения")
        int sortOrder,

        @Schema(description = "Активна ли опция")
        boolean active
) {
}
