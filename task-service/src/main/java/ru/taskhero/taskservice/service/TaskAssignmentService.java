package ru.taskhero.taskservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.dto.TaskAssignRequest;
import ru.taskhero.taskservice.dto.TaskAssignmentResponseDto;
import ru.taskhero.taskservice.dto.TaskReviewRequest;
import ru.taskhero.taskservice.dto.TaskSubmitRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления назначениями заданий.
 */
public interface TaskAssignmentService {

    /**
     * Назначить задание ребёнку.
     *
     * @param parentId ID родителя
     * @param request  данные для назначения
     * @return созданное назначение
     */
    TaskAssignmentResponseDto assign(UUID parentId, TaskAssignRequest request);

    /**
     * Получить назначение по ID.
     *
     * @param id ID назначения
     * @return назначение
     */
    TaskAssignmentResponseDto getById(UUID id);

    /**
     * Получить все назначения ребёнка.
     *
     * @param childId ID ребёнка
     * @return список назначений
     */
    List<TaskAssignmentResponseDto> getByChild(UUID childId);

    /**
     * Получить назначения ребёнка с определённым статусом.
     *
     * @param childId ID ребёнка
     * @param status  статус задания
     * @return список назначений
     */
    List<TaskAssignmentResponseDto> getByChildAndStatus(UUID childId, TaskStatus status);

    /**
     * Получить активные задания ребёнка (CREATED или SUBMITTED).
     *
     * @param childId ID ребёнка
     * @return список активных назначений
     */
    List<TaskAssignmentResponseDto> getActiveByChild(UUID childId);

    /**
     * Получить назначения родителя с пагинацией.
     *
     * @param parentId ID родителя
     * @param pageable параметры пагинации
     * @return страница назначений
     */
    Page<TaskAssignmentResponseDto> getByParent(UUID parentId, Pageable pageable);

    /**
     * Получить назначения на проверку (статус SUBMITTED) для родителя.
     *
     * @param parentId ID родителя
     * @return список назначений на проверку
     */
    List<TaskAssignmentResponseDto> getPendingReviewByParent(UUID parentId);

    /**
     * Сдать задание на проверку (ребёнком).
     *
     * @param assignmentId ID назначения
     * @param childId      ID ребёнка
     * @param request      данные при сдаче
     * @return обновленное назначение
     */
    TaskAssignmentResponseDto submit(UUID assignmentId, UUID childId, TaskSubmitRequest request);

    /**
     * Одобрить выполненное задание (родителем).
     *
     * @param assignmentId ID назначения
     * @param parentId     ID родителя
     * @param request      данные при проверке
     * @return обновленное назначение
     */
    TaskAssignmentResponseDto approve(UUID assignmentId, UUID parentId, TaskReviewRequest request);

    /**
     * Отклонить выполненное задание (родителем).
     *
     * @param assignmentId ID назначения
     * @param parentId     ID родителя
     * @param request      данные при проверке
     * @return обновленное назначение
     */
    TaskAssignmentResponseDto reject(UUID assignmentId, UUID parentId, TaskReviewRequest request);

    /**
     * Получить просроченные задания родителя (статус EXPIRED).
     *
     * @param parentId ID родителя
     * @return список просроченных назначений
     */
    List<TaskAssignmentResponseDto> getExpiredByParent(UUID parentId);

    /**
     * Продлить дедлайн задания.
     *
     * @param assignmentId ID назначения
     * @param parentId     ID родителя
     * @param newDueDate   новый дедлайн
     * @return обновленное назначение
     */
    TaskAssignmentResponseDto extendDeadline(UUID assignmentId, UUID parentId, Instant newDueDate);

    /**
     * Удалить назначение задания.
     *
     * @param assignmentId ID назначения
     * @param parentId     ID родителя
     */
    void deleteAssignment(UUID assignmentId, UUID parentId);

    /**
     * Получить все назначения родителя (для админа, без пагинации).
     */
    List<TaskAssignmentResponseDto> getAllByParent(UUID parentId);

    /**
     * Получить все назначения ребёнка (для админа).
     */
    List<TaskAssignmentResponseDto> getAllByChild(UUID childId);
}
