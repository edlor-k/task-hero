package ru.taskhero.app.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO для добавления ребёнка.
 */
public record CreateChildRequest(
        @NotBlank(message = "Имя обязательно")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        String surname,

        String difficultyTrajectory
) {
}
