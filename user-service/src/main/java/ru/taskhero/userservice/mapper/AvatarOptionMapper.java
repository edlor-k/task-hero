package ru.taskhero.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.taskhero.userservice.dto.AvatarOptionResponseDto;
import ru.taskhero.userservice.entity.AvatarOption;

/**
 * Маппер для сущности AvatarOption.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AvatarOptionMapper {

    AvatarOptionResponseDto toDto(AvatarOption entity);
}
