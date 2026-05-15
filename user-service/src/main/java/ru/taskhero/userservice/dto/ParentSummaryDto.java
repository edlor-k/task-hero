package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Краткая информация о родителе (без коллекций, для безопасной загрузки в деталях ребёнка).
 */
@Schema(description = "Краткая информация о родителе")
public record ParentSummaryDto(

        @Schema(description = "ID родителя")
        UUID id,

        @Schema(description = "ID учётной записи (users)")
        UUID userId,

        @Schema(description = "Имя")
        String firstName,

        @Schema(description = "Фамилия")
        String surname,

        @Schema(description = "Email учётной записи")
        String email
) { }
