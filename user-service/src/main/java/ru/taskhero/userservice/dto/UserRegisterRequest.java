package ru.taskhero.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для регистрации пользователя (родителя/админа)
 */
public record UserRegisterRequest(
        @Email(message = "Некорректный формат email")
        @NotBlank(message = "Email не может быть пустым")
        String email,

        @NotBlank(message = "Пароль не может быть пустым")
        @Size(min = 6, max = 64, message = "Пароль должен содержать от 6 до 64 символов")
        String password,

        @NotBlank(message = "Имя не может быть пустым")
        String firstName,

        @NotBlank(message = "Фамилия не может быть пустой")
        String surname
) { }
