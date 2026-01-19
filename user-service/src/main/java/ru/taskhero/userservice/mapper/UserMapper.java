package ru.taskhero.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.taskhero.userservice.dto.UserRegisterRequest;
import ru.taskhero.userservice.dto.UserResponseDto;
import ru.taskhero.userservice.entity.User;

/**
 * Маппер для конвертации между сущностями и DTO пользователя.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "password", ignore = true) // пароль устанавливается отдельно после кодирования
    @Mapping(target = "role", ignore = true) // роль устанавливается в сервисе
    @Mapping(source = "email", target = "email")
    User toEntity(UserRegisterRequest request);

    @Mapping(source = "active", target = "isActive")
    UserResponseDto toDto(User user);
}
