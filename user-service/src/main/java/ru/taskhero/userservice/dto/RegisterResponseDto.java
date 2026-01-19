package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.taskhero.common.model.enums.Role;

import java.util.UUID;

/**
 * DTO для ответа при регистрации нового родителя.
 * Объединяет данные User и Parent профиля.
 */
@Schema(description = "Ответ с полной информацией о зарегистрированном родителе")
public record RegisterResponseDto(

        @Schema(description = "Идентификатор пользователя")
        UUID userId,

        @Schema(description = "Email пользователя")
        String email,

        @Schema(description = "Роль пользователя")
        Role role,

        @Schema(description = "Идентификатор профиля родителя")
        UUID parentId,

        @Schema(description = "Имя родителя")
        String firstName,

        @Schema(description = "Фамилия родителя")
        String surname,

        @Schema(description = "Активен ли пользователь")
        boolean isActive
) {}
