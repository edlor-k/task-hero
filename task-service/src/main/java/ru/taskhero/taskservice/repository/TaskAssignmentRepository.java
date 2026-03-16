package ru.taskhero.taskservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.entity.TaskAssignment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с назначениями заданий.
 */
@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {

    /**
     * Найти все назначения ребёнка.
     *
     * @param childId ID ребёнка
     * @return список назначений
     */
    List<TaskAssignment> findAllByChildId(UUID childId);

    /**
     * Найти все назначения ребёнка с пагинацией.
     *
     * @param childId  ID ребёнка
     * @param pageable параметры пагинации
     * @return страница назначений
     */
    Page<TaskAssignment> findAllByChildId(UUID childId, Pageable pageable);

    /**
     * Найти все назначения ребёнка с определённым статусом.
     *
     * @param childId ID ребёнка
     * @param status  статус задания
     * @return список назначений
     */
    List<TaskAssignment> findAllByChildIdAndStatus(UUID childId, TaskStatus status);

    /**
     * Найти назначения ребёнка с определёнными статусами.
     *
     * @param childId  ID ребёнка
     * @param statuses список статусов
     * @return список назначений
     */
    List<TaskAssignment> findAllByChildIdAndStatusIn(UUID childId, List<TaskStatus> statuses);

    /**
     * Найти все назначения на основе шаблона.
     *
     * @param templateId ID шаблона
     * @return список назначений
     */
    List<TaskAssignment> findAllByTemplateId(UUID templateId);

    /**
     * Найти назначения по шаблонам родителя.
     *
     * @param parentId ID родителя
     * @param pageable параметры пагинации
     * @return страница назначений
     */
    @Query("SELECT a FROM TaskAssignment a WHERE a.template.parentId = :parentId")
    Page<TaskAssignment> findAllByParentId(@Param("parentId") UUID parentId, Pageable pageable);

    /**
     * Найти назначения по шаблонам родителя с определённым статусом.
     *
     * @param parentId ID родителя
     * @param status   статус задания
     * @return список назначений
     */
    @Query("SELECT a FROM TaskAssignment a WHERE a.template.parentId = :parentId AND a.status = :status")
    List<TaskAssignment> findAllByParentIdAndStatus(
            @Param("parentId") UUID parentId,
            @Param("status") TaskStatus status
    );

    /**
     * Найти назначение по ID и ID ребёнка.
     *
     * @param id      ID назначения
     * @param childId ID ребёнка
     * @return назначение или empty
     */
    Optional<TaskAssignment> findByIdAndChildId(UUID id, UUID childId);

    /**
     * Найти назначение по ID с загруженным шаблоном.
     *
     * @param id ID назначения
     * @return назначение или empty
     */
    @Query("SELECT a FROM TaskAssignment a JOIN FETCH a.template WHERE a.id = :id")
    Optional<TaskAssignment> findByIdWithTemplate(@Param("id") UUID id);

    /**
     * Найти просроченные назначения.
     *
     * @param date   дата для сравнения
     * @param status статус задания
     * @return список просроченных назначений
     */
    List<TaskAssignment> findAllByDueDateBeforeAndStatus(LocalDate date, TaskStatus status);

    /**
     * Подсчитать количество назначений ребёнка с определённым статусом.
     *
     * @param childId ID ребёнка
     * @param status  статус задания
     * @return количество назначений
     */
    long countByChildIdAndStatus(UUID childId, TaskStatus status);

    /**
     * Проверить, существует ли назначение для ребёнка по шаблону со статусом.
     *
     * @param childId    ID ребёнка
     * @param templateId ID шаблона
     * @param statuses   список статусов
     * @return true если существует
     */
    boolean existsByChildIdAndTemplateIdAndStatusIn(UUID childId, UUID templateId, List<TaskStatus> statuses);
}
