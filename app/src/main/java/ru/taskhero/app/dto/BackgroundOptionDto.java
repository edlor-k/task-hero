package ru.taskhero.app.dto;

import java.util.UUID;

public record BackgroundOptionDto(
        UUID id,
        String imageUrl,
        String label,
        int sortOrder,
        boolean active
) {
}
