package ru.taskhero.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequestDto(
        @NotBlank @Email String email,
        String firstName,
        String surname
) {}
