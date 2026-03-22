package ru.taskhero.app.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO родителя с детьми (из admin API).
 */
public record AdminParentDto(
        UUID id,
        String firstName,
        String surname,
        AdminUserDto user,
        List<ChildDto> children,
        int totalChildren
) { }
