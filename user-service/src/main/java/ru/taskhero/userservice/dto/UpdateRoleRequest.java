package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ru.taskhero.common.model.enums.Role;

/**
 * Запрос на изменение роли пользователя.
 */
@Schema(description = "Запрос на изменение роли пользователя")
public record UpdateRoleRequest(

        @NotNull(message = "Роль обязательна")
        @Schema(description = "Новая роль пользователя", example = "PARENT")
        Role newRole
) { }
