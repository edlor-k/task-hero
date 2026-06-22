package ru.taskhero.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
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
    @Mapping(target = "nickname", source = "entity.nickname")
    @Mapping(target = "nicknameUnlocked", source = "entity.nicknameUnlocked")
    @Mapping(target = "expToNextLevel", source = "expToNextLevel")
    @Mapping(target = "currentLevelExp", source = "currentLevelExp")
    @Mapping(target = "nextLevelExp", source = "nextLevelExp")
    @Mapping(target = "avatarUrl", source = "entity", qualifiedByName = "avatarUrl")
    @Mapping(target = "avatarOptionId", source = "entity", qualifiedByName = "avatarOptionId")
    @Mapping(target = "backgroundUrl", source = "entity", qualifiedByName = "backgroundUrl")
    @Mapping(target = "backgroundOptionId", source = "entity", qualifiedByName = "backgroundOptionId")
    @Mapping(target = "accentColor", source = "entity.accentColor")
    @Mapping(target = "parent", source = "parent")
    ChildResponseDto toDto(Child entity, int expToNextLevel, int currentLevelExp, int nextLevelExp, ParentResponseDto parent);

    /**
     * Упрощённое преобразование без EXP-прогресса (для списков, где EXP не важен).
     */
    default ChildResponseDto toDto(Child entity) {
        return toDto(entity, 0, 0, 0, null);
    }

    /**
     * URL аватара ребёнка — берётся из выбранной опции галереи, {@code null},
     * если аватар ещё не выбран.
     */
    @Named("avatarUrl")
    default String avatarUrl(Child entity) {
        return entity.getAvatarOption() != null ? entity.getAvatarOption().getImageUrl() : null;
    }

    /**
     * ID выбранной опции аватара ({@code null}, если ещё не выбран).
     */
    @Named("avatarOptionId")
    default java.util.UUID avatarOptionId(Child entity) {
        return entity.getAvatarOption() != null ? entity.getAvatarOption().getId() : null;
    }

    @Named("backgroundUrl")
    default String backgroundUrl(Child entity) {
        return entity.getBackgroundOption() != null ? entity.getBackgroundOption().getImageUrl() : null;
    }

    @Named("backgroundOptionId")
    default java.util.UUID backgroundOptionId(Child entity) {
        return entity.getBackgroundOption() != null ? entity.getBackgroundOption().getId() : null;
    }
}
