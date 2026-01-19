package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
        String loginToken
) {}
