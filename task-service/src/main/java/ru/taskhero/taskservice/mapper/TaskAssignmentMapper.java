package ru.taskhero.taskservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.taskhero.taskservice.dto.TaskAssignmentResponseDto;
import ru.taskhero.taskservice.entity.TaskAssignment;

/**
 * Маппер для преобразования между TaskAssignment и DTO.
 */
@Mapper(
        componentModel = "spring",
        uses = {TaskTemplateMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskAssignmentMapper {

    /**
     * Преобразование сущности в DTO ответа.
     *
     * @param entity сущность назначения
     * @return DTO ответа
     */
    TaskAssignmentResponseDto toDto(TaskAssignment entity);
}
