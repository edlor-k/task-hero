package ru.taskhero.taskservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.taskhero.common.model.enums.TaskCategory;
import ru.taskhero.taskservice.dto.TaskTemplateCreateRequest;
import ru.taskhero.taskservice.dto.TaskTemplateResponseDto;
import ru.taskhero.taskservice.dto.TaskTemplateUpdateRequest;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления шаблонами заданий.
 */
public interface TaskTemplateService {

    /**
     * Создать новый шаблон задания.
     *
     * @param parentId ID родителя
     * @param request  данные для создания шаблона
     * @return созданный шаблон
     */
    TaskTemplateResponseDto create(UUID parentId, TaskTemplateCreateRequest request);

    /**
     * Обновить существующий шаблон задания.
     *
     * @param id       ID шаблона
     * @param parentId ID родителя (для проверки владения)
     * @param request  данные для обновления
     * @return обновленный шаблон
     */
    TaskTemplateResponseDto update(UUID id, UUID parentId, TaskTemplateUpdateRequest request);

    /**
     * Удалить шаблон задания.
     *
     * @param id       ID шаблона
     * @param parentId ID родителя (для проверки владения)
     */
    void delete(UUID id, UUID parentId);

    /**
     * Получить шаблон по ID.
     *
     * @param id ID шаблона
     * @return шаблон
     */
    TaskTemplateResponseDto getById(UUID id);

    /**
     * Получить все шаблоны родителя с пагинацией.
     *
     * @param parentId ID родителя
     * @param pageable параметры пагинации
     * @return страница шаблонов
     */
    Page<TaskTemplateResponseDto> getByParent(UUID parentId, Pageable pageable);

    /**
     * Получить все активные шаблоны родителя.
     *
     * @param parentId ID родителя
     * @return список активных шаблонов
     */
    List<TaskTemplateResponseDto> getActiveByParent(UUID parentId);

    /**
     * Получить все шаблоны из библиотеки.
     *
     * @return список шаблонов библиотеки
     */
    List<TaskTemplateResponseDto> getLibraryTemplates();

    /**
     * Получить шаблоны библиотеки по категории.
     *
     * @param category категория
     * @return список шаблонов библиотеки
     */
    List<TaskTemplateResponseDto> getLibraryTemplatesByCategory(TaskCategory category);

    /**
     * Скопировать шаблон из библиотеки родителю.
     *
     * @param libraryTemplateId ID шаблона библиотеки
     * @param parentId          ID родителя
     * @return скопированный шаблон
     */
    TaskTemplateResponseDto copyFromLibrary(UUID libraryTemplateId, UUID parentId);
}
