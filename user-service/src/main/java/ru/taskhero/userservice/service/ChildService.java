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
}
