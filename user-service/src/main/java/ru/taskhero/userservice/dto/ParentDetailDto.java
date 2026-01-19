package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Детальная информация о родителе для админ-панели.
 */
@Schema(description = "Детальная информация о родителе")
public record ParentDetailDto(

        @Schema(description = "ID профиля родителя")
        UUID id,

        @Schema(description = "Имя родителя")
        String firstName,

        @Schema(description = "Фамилия родителя")
        String surname,

        @Schema(description = "Информация о пользователе")
        UserResponseDto user,

        @Schema(description = "Список детей")
        List<ChildResponseDto> children,

        @Schema(description = "Дата создания профиля")
        Instant createdAt,

        @Schema(description = "Дата последнего обновления")
        Instant updatedAt
) {}
