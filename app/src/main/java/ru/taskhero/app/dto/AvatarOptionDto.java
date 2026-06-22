package ru.taskhero.app.dto;

import java.util.UUID;

/**
 * DTO опции аватара из галереи (зеркало user-service AvatarOptionResponseDto).
 */
public record AvatarOptionDto(
        UUID id,
        String imageUrl,
        String label,
        int sortOrder,
        boolean active
) {
}
