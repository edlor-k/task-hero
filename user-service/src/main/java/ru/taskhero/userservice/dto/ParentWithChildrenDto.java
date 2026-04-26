package ru.taskhero.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * DTO родителя с информацией о его детях.
 */
@Schema(description = "Родитель с информацией о детях")
public record ParentWithChildrenDto(

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

        @Schema(description = "Общее количество детей")
        int totalChildren
) { }
