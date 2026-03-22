package ru.taskhero.app.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO детальной информации о родителе (из admin API).
 */
public record AdminParentDetailDto(
        UUID id,
        String firstName,
        String surname,
        AdminUserDto user,
        List<ChildDto> children,
        Instant createdAt,
        Instant updatedAt
) { }
