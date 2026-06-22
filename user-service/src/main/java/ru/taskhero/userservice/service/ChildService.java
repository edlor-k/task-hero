package ru.taskhero.userservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.taskhero.userservice.dto.ChildCreateRequestDto;
import ru.taskhero.userservice.dto.ChildDetailDto;
import ru.taskhero.userservice.dto.ChildResponseDto;
import ru.taskhero.userservice.dto.UpdateChildRequest;

import java.util.List;
import java.util.UUID;

public interface ChildService {

    /**
     * Добавить нового ребёнка к родителю.
     */
    ChildResponseDto createChild(UUID parentId, ChildCreateRequestDto request);

    /**
     * Получить всех детей родителя.
     */
    List<ChildResponseDto> getChildrenByParent(UUID parentId);

    /**
     * Найти ребёнка по loginToken.
     */
    ChildResponseDto getByLoginToken(String token);

    /**
     * Получить всех детей в системе с пагинацией (для админов).
     */
    Page<ChildResponseDto> getAllChildren(Pageable pageable);

    /**
     * Получить детальную информацию о ребенке по ID.
     */
    ChildDetailDto getDetailById(UUID childId);

    /**
     * Получить ребенка по ID.
     */
    ChildResponseDto getById(UUID childId);

    /**
     * Обновить данные ребенка.
     */
    ChildResponseDto updateChild(UUID childId, UpdateChildRequest request);

    /**
     * Удалить ребенка.
     */
    void deleteChild(UUID childId);

    /**
     * Проверка, принадлежит ли ребенок родителю.
     */
    boolean isChildBelongsToParent(UUID childId, UUID parentId);

    /**
     * Начислить награду ребёнку (EXP и коины).
     *
     * @param childId ID ребёнка
     * @param exp     количество EXP для начисления
     * @param coins   количество коинов для начисления
     * @return обновленные данные ребёнка
     */
    ChildResponseDto addReward(UUID childId, int exp, int coins);

    /**
     * Начислить награду ребёнку с возможным ограничением EXP текущим уровнем.
     */
    ChildResponseDto addReward(UUID childId, int exp, int coins, boolean capExp);

    /**
     * Выбрать аватар из галереи для ребёнка (при первом входе или позже).
     *
     * @param childId       ID ребёнка
     * @param avatarOptionId ID опции аватара из галереи
     * @return обновленные данные ребёнка
     */
    ChildResponseDto selectAvatar(UUID childId, UUID avatarOptionId);

    /**
     * Выбрать фон из галереи (при первом входе или позже начиная с уровня 3).
     */
    ChildResponseDto selectBackground(UUID childId, UUID backgroundOptionId);

    /**
     * Поиск детей по имени или фамилии.
     */
    Page<ChildResponseDto> searchChildren(String query, Pageable pageable);

    /**
     * Разблокировать изменение никнейма ребёнка (вызывается после выполнения первого задания).
     */
    void unlockNickname(UUID childId);

    /**
     * Обновить никнейм ребёнка (доступно только после разблокировки).
     */
    ChildResponseDto updateNickname(UUID childId, String nickname);

    /**
     * Выбрать акцентный цвет UI для ребёнка (доступно всегда, без ограничений по уровню).
     */
    ChildResponseDto selectAccentColor(UUID childId, String accentColor);

    /**
     * Принудительно (без проверки уровня) установить аватар ребёнку — только для администратора.
     */
    ChildResponseDto adminForceSetAvatar(UUID childId, UUID avatarOptionId);

    /**
     * Принудительно (без проверки уровня) установить фон ребёнку — только для администратора.
     */
    ChildResponseDto adminForceSetBackground(UUID childId, UUID backgroundOptionId);
}
