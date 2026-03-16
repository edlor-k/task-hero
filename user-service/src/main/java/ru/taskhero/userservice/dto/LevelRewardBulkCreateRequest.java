package ru.taskhero.userservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LevelRewardBulkCreateRequest(
        @NotEmpty @Valid List<LevelRewardCreateRequest> rewards
) {
}
