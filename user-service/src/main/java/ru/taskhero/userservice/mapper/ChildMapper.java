package ru.taskhero.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import ru.taskhero.common.model.enums.CharacterType;
import ru.taskhero.userservice.dto.ChildCreateRequestDto;
import ru.taskhero.userservice.dto.ChildResponseDto;
import ru.taskhero.userservice.dto.ParentResponseDto;
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
     * Поля expToNextLevel, currentLevelExp, nextLevelExp заполняются из параметров.
     */
    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "firstName", source = "entity.firstName")
    @Mapping(target = "surname", source = "entity.surname")
    @Mapping(target = "expToNextLevel", source = "expToNextLevel")
    @Mapping(target = "currentLevelExp", source = "currentLevelExp")
    @Mapping(target = "nextLevelExp", source = "nextLevelExp")
    @Mapping(target = "characterImagePath", source = "entity", qualifiedByName = "characterImagePath")
    @Mapping(target = "parent", source = "parent")
    ChildResponseDto toDto(Child entity, int expToNextLevel, int currentLevelExp, int nextLevelExp, ParentResponseDto parent);

    /**
     * Упрощённое преобразование без EXP-прогресса (для списков, где EXP не важен).
     */
    default ChildResponseDto toDto(Child entity) {
        return toDto(entity, 0, 0, 0, null);
    }

    /**
     * Вычислить путь к изображению персонажа.
     */
    @Named("characterImagePath")
    default String characterImagePath(Child entity) {
        if (entity.getCharacterType() == null) {
            return null;
        }
        return entity.getCharacterType().getImagePath(entity.getLevel());
    }
}
