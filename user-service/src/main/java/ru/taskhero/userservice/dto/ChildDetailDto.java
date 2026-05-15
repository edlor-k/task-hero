package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.taskhero.common.model.enums.CharacterType;
import ru.taskhero.common.model.enums.DifficultyTrajectory;

import java.time.Instant;
import java.util.UUID;

/**
 * Детальная информация о ребенке для админ-панели.
 */
@Schema(description = "Детальная информация о ребенке")
public record ChildDetailDto(

        @Schema(description = "ID ребенка")
        UUID id,

        @Schema(description = "Имя")
        String firstName,

        @Schema(description = "Фамилия")
        String surname,

        @Schema(description = "Токен для входа")
        String loginToken,

        @Schema(description = "Опыт")
        int exp,

        @Schema(description = "Коины")
        int coins,

        @Schema(description = "Уровень")
        int level,

        @Schema(description = "Траектория сложности")
        DifficultyTrajectory difficultyTrajectory,

        @Schema(description = "Тип персонажа")
        CharacterType characterType,

        @Schema(description = "Информация о родителе")
        ParentSummaryDto parent,

        @Schema(description = "Дата создания")
        Instant createdAt,

        @Schema(description = "Дата последнего обновления")
        Instant updatedAt,

        @Schema(description = "Никнейм ребёнка")
        String nickname
) { }
