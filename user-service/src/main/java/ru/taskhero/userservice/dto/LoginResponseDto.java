package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ответ при успешной авторизации.
 */
@Schema(description = "Ответ при успешной авторизации")
public record LoginResponseDto(

        @Schema(description = "JWT-токен для доступа")
        String accessToken,

        @Schema(description = "Имя пользователя/ребёнка")
        String displayName,

        @Schema(description = "Роль пользователя")
        String role
) { }
