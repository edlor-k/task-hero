package ru.taskhero.userservice.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO для возврата информации о родителе.
 */
public record ParentResponseDto(
        UUID id,
        UUID userId,
        String firstName,
        String surname,
        List<UUID> childIds
) {}
