package ru.taskhero.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.taskhero.userservice.dto.ChildCreateRequestDto;
import ru.taskhero.userservice.dto.ChildResponseDto;
import ru.taskhero.userservice.entity.Child;

/**
 * Маппер для сущности Child и связанных DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChildMapper {

    /**
     * Преобразует DTO запроса в сущность Child.
     */
    Child toEntity(ChildCreateRequestDto request);

    /**
     * Преобразует сущность Child в DTO для ответа.
     */
    ChildResponseDto toDto(Child entity);
}
