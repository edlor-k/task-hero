package ru.taskhero.taskservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.taskhero.taskservice.dto.TaskSubItemDto;
import ru.taskhero.taskservice.dto.TaskTemplateCreateRequest;
import ru.taskhero.taskservice.dto.TaskTemplateResponseDto;
import ru.taskhero.taskservice.dto.TaskTemplateUpdateRequest;
import ru.taskhero.taskservice.entity.TaskSubItem;
import ru.taskhero.taskservice.entity.TaskTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Маппер для преобразования между TaskTemplate и DTO.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskTemplateMapper {

    /**
     * Преобразование сущности в DTO ответа.
     */
    @Mapping(target = "subItems", source = "subItems", qualifiedByName = "subItemsToDto")
    TaskTemplateResponseDto toDto(TaskTemplate entity);

    /**
     * Создание сущности из запроса на создание.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "subItems", ignore = true)
    @Mapping(target = "expReward", defaultValue = "10")
    @Mapping(target = "coinsReward", defaultValue = "5")
    @Mapping(target = "repeatable", defaultValue = "false")
    @Mapping(target = "libraryTemplate", constant = "false")
    @Mapping(target = "sourceTemplateId", ignore = true)
    @Mapping(target = "difficulty", defaultExpression = "java(ru.taskhero.common.model.enums.TaskDifficulty.NORMAL)")
    TaskTemplate toEntity(TaskTemplateCreateRequest request);

    /**
     * Обновление сущности из запроса на обновление.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "subItems", ignore = true)
    @Mapping(target = "libraryTemplate", ignore = true)
    @Mapping(target = "sourceTemplateId", ignore = true)
    void updateFromRequest(TaskTemplateUpdateRequest request, @MappingTarget TaskTemplate entity);

    /**
     * Маппинг подпунктов в DTO.
     */
    @Named("subItemsToDto")
    default List<TaskSubItemDto> subItemsToDto(List<TaskSubItem> subItems) {
        if (subItems == null) {
            return Collections.emptyList();
        }
        return subItems.stream()
                .map(si -> new TaskSubItemDto(si.getId(), si.getTitle(), si.getOrderIndex()))
                .toList();
    }
}
