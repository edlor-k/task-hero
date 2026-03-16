package ru.taskhero.taskservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.taskhero.common.model.enums.TaskCategory;
import ru.taskhero.taskservice.entity.TaskTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с шаблонами заданий.
 */
@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, UUID> {

    /**
     * Найти все шаблоны родителя с пагинацией.
     *
     * @param parentId ID родителя
     * @param pageable параметры пагинации
     * @return страница шаблонов
     */
    Page<TaskTemplate> findAllByParentId(UUID parentId, Pageable pageable);

    /**
     * Найти все шаблоны родителя.
     *
     * @param parentId ID родителя
     * @return список шаблонов
     */
    List<TaskTemplate> findAllByParentId(UUID parentId);

    /**
     * Найти все активные шаблоны родителя.
     *
     * @param parentId ID родителя
     * @param active   статус активности
     * @return список шаблонов
     */
    List<TaskTemplate> findAllByParentIdAndActive(UUID parentId, boolean active);

    /**
     * Найти шаблон по ID и ID родителя.
     *
     * @param id       ID шаблона
     * @param parentId ID родителя
     * @return шаблон или empty
     */
    Optional<TaskTemplate> findByIdAndParentId(UUID id, UUID parentId);

    /**
     * Проверить существование шаблона с указанным ID и родителем.
     *
     * @param id       ID шаблона
     * @param parentId ID родителя
     * @return true если шаблон существует
     */
    boolean existsByIdAndParentId(UUID id, UUID parentId);

    /**
     * Найти все шаблоны библиотеки.
     *
     * @return список шаблонов библиотеки
     */
    List<TaskTemplate> findAllByLibraryTemplateTrue();

    /**
     * Найти шаблоны библиотеки по категории.
     *
     * @param category категория
     * @return список шаблонов библиотеки
     */
    List<TaskTemplate> findAllByLibraryTemplateTrueAndCategory(TaskCategory category);
}
