package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO для входа ребёнка по токену.
 */
@Schema(description = "Запрос на вход ребёнка по токену")
public record ChildLoginRequestDto(

        @NotBlank(message = "Токен входа обязателен")
        @Schema(description = "Токен для входа ребёнка", example = "f87c5a9c8c3d4e3b8a6b7f")
        String loginToken
) {}
