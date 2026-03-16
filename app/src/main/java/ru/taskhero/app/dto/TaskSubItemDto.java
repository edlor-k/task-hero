package ru.taskhero.app.dto;

import java.util.UUID;

/**
 * DTO подпункта задания.
 */
public record TaskSubItemDto(
        UUID id,
        String title,
        int orderIndex
) {
}
