package ru.taskhero.userservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.taskhero.userservice.dto.ParentDetailDto;
import ru.taskhero.userservice.dto.ParentResponseDto;
import ru.taskhero.userservice.dto.ParentWithChildrenDto;
import ru.taskhero.userservice.dto.UpdateParentRequest;

import java.util.UUID;

public interface ParentService {

    /**
     * Создание профиля родителя на основе User ID.
     */
    ParentResponseDto createForUser(UUID userId, String firstName, String surname);

    /**
     * Получение информации о родителе по User ID.
     */
    ParentResponseDto getByUserId(UUID userId);

    /**
     * Получить всех родителей с информацией о детях (с пагинацией).
     */
    Page<ParentWithChildrenDto> getAllParents(Pageable pageable);

    /**
     * Получить детальную информацию о родителе по ID.
     */
    ParentDetailDto getDetailById(UUID parentId);

    /**
     * Обновить профиль родителя.
     */
    ParentResponseDto updateParent(UUID parentId, UpdateParentRequest request);

    /**
     * Обновить профиль текущего родителя.
     */
    ParentResponseDto updateCurrentParent(UUID userId, UpdateParentRequest request);

    /**
     * Поиск родителей по имени, фамилии или email.
     */
    Page<ParentWithChildrenDto> searchParents(String query, Pageable pageable);
}
