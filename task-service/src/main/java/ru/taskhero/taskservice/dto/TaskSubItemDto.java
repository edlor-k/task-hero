package ru.taskhero.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO подпункта задания.
 */
@Schema(description = "Подпункт задания")
public record TaskSubItemDto(

        @Schema(description = "ID подпункта")
        UUID id,

        @Schema(description = "Название подпункта")
        String title,

        @Schema(description = "Порядковый номер")
        int orderIndex
) {
}
