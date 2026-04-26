package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на обновление данных родителя.
 */
@Schema(description = "Запрос на обновление профиля родителя")
public record UpdateParentRequest(

        @NotBlank(message = "Имя обязательно")
        @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
        @Schema(description = "Имя родителя", example = "Иван")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        @Size(min = 2, max = 50, message = "Фамилия должна быть от 2 до 50 символов")
        @Schema(description = "Фамилия родителя", example = "Иванов")
        String surname
) { }
