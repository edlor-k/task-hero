package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.taskhero.common.model.enums.CharacterType;
import ru.taskhero.common.model.enums.DifficultyTrajectory;

import java.util.UUID;

/**
 * DTO для ответа при создании или получении ребёнка.
 */
@Schema(description = "Ответ с информацией о ребёнке")
public record ChildResponseDto(
        @Schema(description = "Идентификатор ребёнка")
        UUID id,
        @Schema(description = "Имя ребёнка")
        String firstName,
        @Schema(description = "Фамилия ребёнка")
        String surname,
        @Schema(description = "Опыт (EXP)")
        int exp,
        @Schema(description = "Количество монет")
        int coins,
        @Schema(description = "Уровень")
        int level,
        @Schema(description = "Ссылка на аватар")
        String avatarUrl,
        @Schema(description = "Уникальный токен для входа ребёнка")
        String loginToken,
        @Schema(description = "Траектория сложности")
        DifficultyTrajectory difficultyTrajectory,
        @Schema(description = "Тип персонажа")
        CharacterType characterType,
        @Schema(description = "Выбран ли персонаж")
        boolean characterSelected,
        @Schema(description = "Путь к изображению персонажа")
        String characterImagePath,
        @Schema(description = "EXP до следующего уровня")
        int expToNextLevel,
        @Schema(description = "EXP текущего уровня (начало)")
        int currentLevelExp,
        @Schema(description = "EXP следующего уровня (порог)")
        int nextLevelExp,
        @Schema(description = "Информация о родителе")
        ParentResponseDto parent,

        @Schema(description = "Никнейм ребёнка (отображается вместо имени в публичном UI)")
        String nickname
) { }
