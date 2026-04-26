package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO для входа родителя или администратора.
 */
@Schema(description = "Запрос на вход родителя или администратора")
public record UserLoginRequestDto(

        @Email(message = "Некорректный формат email")
        @NotBlank(message = "Email не может быть пустым")
        @Schema(description = "Email пользователя", example = "parent@example.com")
        String email,

        @NotBlank(message = "Пароль не может быть пустым")
        @Schema(description = "Пароль пользователя", example = "secret123")
        String password
) { }
