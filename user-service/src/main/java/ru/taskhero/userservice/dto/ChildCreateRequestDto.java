package ru.taskhero.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.taskhero.userservice.entity.Child;

/**
 * DTO for {@link Child}
 */
public record ChildCreateRequestDto(
        @Size(message = "Имя должно содержать от 2 до 64 символов", min = 2, max = 64)
        @NotBlank(message = "Имя не может быть пустым")
        String firstName,

        @Size(message = "Фамилия должна содержать от 2 до 64 символов", min = 2, max = 64)
        @NotBlank(message = "Фамилия не может быть пустой")
        String surname
) {
}
