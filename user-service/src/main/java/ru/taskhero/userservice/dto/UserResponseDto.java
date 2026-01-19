package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.taskhero.common.model.enums.Role;

import java.util.UUID;

/**
 * DTO для представления данных пользователя (User entity).
 * Для получения данных родителя используйте ParentResponseDto.
 * Для получения данных ребенка используйте ChildResponseDto.
 */
@Schema(description = "Ответ с информацией о пользователе")
public record UserResponseDto(

        @Schema(description = "Идентификатор пользователя")
        UUID id,

        @Schema(description = "Email пользователя")
        String email,

        @Schema(description = "Роль пользователя")
        Role role,

        @Schema(description = "Активен ли пользователь")
        boolean isActive
) {}
