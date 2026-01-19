package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Детальная информация о ребенке для админ-панели.
 */
@Schema(description = "Детальная информация о ребенке")
public record ChildDetailDto(

        @Schema(description = "ID ребенка")
        UUID id,

        @Schema(description = "Имя ребенка")
        String firstName,


        @Schema(description = "Токен для входа")
        String loginToken,

        @Schema(description = "Опыт")
        int exp,

        @Schema(description = "Монеты")
        int coins,

        @Schema(description = "Уровень")
        int level,

        @Schema(description = "Информация о родителе")
        ParentResponseDto parent,

        @Schema(description = "Дата создания")
        Instant createdAt,

        @Schema(description = "Дата последнего обновления")
        Instant updatedAt
) {}
