package ru.taskhero.userservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LevelRewardCreateRequest(
        @Min(2) int level,
        @NotBlank @Size(max = 128) String title,
        @Size(max = 512) String description,
        UUID shopItemId
) {
}
