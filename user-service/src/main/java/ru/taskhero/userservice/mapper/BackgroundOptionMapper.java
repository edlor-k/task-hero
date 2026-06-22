package ru.taskhero.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.taskhero.userservice.dto.BackgroundOptionResponseDto;
import ru.taskhero.userservice.entity.BackgroundOption;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BackgroundOptionMapper {

    BackgroundOptionResponseDto toDto(BackgroundOption entity);
}
