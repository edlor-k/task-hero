package ru.taskhero.taskservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.aop.LogMethod;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.UnauthorizedException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.dto.TaskAssignRequest;
import ru.taskhero.taskservice.dto.TaskAssignmentResponseDto;
import ru.taskhero.taskservice.dto.TaskReviewRequest;
import ru.taskhero.taskservice.dto.TaskSubmitRequest;
import ru.taskhero.taskservice.entity.TaskAssignment;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.mapper.TaskAssignmentMapper;
import ru.taskhero.taskservice.repository.TaskAssignmentRepository;
import ru.taskhero.taskservice.repository.TaskTemplateRepository;
import ru.taskhero.taskservice.service.RewardService;
import ru.taskhero.taskservice.service.TaskAssignmentService;
import ru.taskhero.taskservice.service.TaskTemplateService;
import ru.taskhero.taskservice.client.UserServiceClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Реализация сервиса для управления назначениями заданий.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAssignmentServiceImpl implements TaskAssignmentService {

    private final TaskAssignmentRepository assignmentRepository;
    private final TaskTemplateRepository templateRepository;
    private final TaskAssignmentMapper assignmentMapper;
    private final RewardService rewardService;
    private final TaskTemplateService templateService;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    @LogMethod("assignment-assign")
    public TaskAssignmentResponseDto assign(UUID parentId, TaskAssignRequest request) {
        log.info("Назначение задания {} ребёнку {} от родителя {}",
                request.templateId(), request.childId(), parentId);

        // Проверяем, что шаблон принадлежит родителю
        TaskTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> {
                    log.error("Шаблон {} не найден", request.templateId());
                    return new ResourceNotFoundException("Шаблон не найден");
                });

        if (!template.getParentId().equals(parentId)) {
            log.warn("Родитель {} пытается назначить чужой шаблон {}", parentId, request.templateId());
            throw new UnauthorizedException("Вы не можете использовать этот шаблон");
        }

        if (!template.isActive()) {
            log.warn("Попытка назначить неактивный шаблон {}", request.templateId());
            throw new ValidationException("Шаблон неактивен");
        }

        // Проверяем, нет ли уже активного задания по этому шаблону
        List<TaskStatus> activeStatuses = List.of(TaskStatus.CREATED, TaskStatus.SUBMITTED);
        boolean hasActiveAssignment = assignmentRepository.existsByChildIdAndTemplateIdAndStatusIn(
                request.childId(), request.templateId(), activeStatuses
        );

        if (hasActiveAssignment && !template.isRepeatable()) {
            log.warn("У ребёнка {} уже есть активное задание по шаблону {}", request.childId(), request.templateId());
            throw new ValidationException("У ребёнка уже есть активное задание по этому шаблону");
        }

        // Создаем назначение
        TaskAssignment assignment = TaskAssignment.builder()
                .template(template)
                .childId(request.childId())
                .status(TaskStatus.CREATED)
                .dueDate(request.dueDate())
                .important(request.important() != null && request.important())
                .build();

        assignment = assignmentRepository.save(assignment);
        log.info("Задание назначено с ID: {}", assignment.getId());

        // Автоподтверждение: если шаблон помечен autoSubmit, сразу начислить награду
        if (template.isAutoSubmit()) {
            int expToGrant = template.getExpReward();
            int coinsToGrant = template.getCoinsReward();
            assignment.setStatus(TaskStatus.APPROVED);
            assignment.setReviewedAt(Instant.now());
            assignment.setExpEarned(expToGrant);
            assignment.setCoinsEarned(coinsToGrant);
            rewardService.grantReward(request.childId(), expToGrant, coinsToGrant, false);
            log.info("Задание {} автоподтверждено: {} EXP, {} коинов", assignment.getId(), expToGrant, coinsToGrant);
        }

        return assignmentMapper.toDto(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("assignment-get-by-id")
    public TaskAssignmentResponseDto getById(UUID id) {
        log.debug("Получение назначения по ID: {}", id);

        TaskAssignment assignment = assignmentRepository.findByIdWithTemplate(id)
                .orElseThrow(() -> {
                    log.error("Назначение {} не найдено", id);
                    return new ResourceNotFoundException("Назначение не найдено");
                });

        return assignmentMapper.toDto(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("assignment-get-by-child")
    public List<TaskAssignmentResponseDto> getByChild(UUID childId) {
        log.debug("Получение всех заданий ребёнка: {}", childId);

        return assignmentRepository.findAllByChildIdWithTemplate(childId)
                .stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("assignment-get-by-child-status")
    public List<TaskAssignmentResponseDto> getByChildAndStatus(UUID childId, TaskStatus status) {
        log.debug("Получение заданий ребёнка {} со статусом {}", childId, status);

        return assignmentRepository.findAllByChildIdAndStatusWithTemplate(childId, status)
                .stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("assignment-get-active-by-child")
    public List<TaskAssignmentResponseDto> getActiveByChild(UUID childId) {
        log.debug("Получение активных заданий ребёнка: {}", childId);

        List<TaskStatus> activeStatuses = List.of(TaskStatus.CREATED, TaskStatus.SUBMITTED);

        return assignmentRepository.findAllByChildIdAndStatusInWithTemplate(childId, activeStatuses)
                .stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("assignment-get-by-parent")
    public Page<TaskAssignmentResponseDto> getByParent(UUID parentId, Pageable pageable) {
        log.debug("Получение заданий родителя: {}", parentId);

        return assignmentRepository.findAllByParentIdWithTemplate(parentId, pageable)
                .map(assignmentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("assignment-get-pending-review")
    public List<TaskAssignmentResponseDto> getPendingReviewByParent(UUID parentId) {
        log.debug("Получение заданий на проверку для родителя: {}", parentId);

        return assignmentRepository.findAllByParentIdAndStatusWithTemplate(parentId, TaskStatus.SUBMITTED)
                .stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    @LogMethod("assignment-submit")
    public TaskAssignmentResponseDto submit(UUID assignmentId, UUID childId, TaskSubmitRequest request) {
        log.info("Сдача задания {} ребёнком {}", assignmentId, childId);

        TaskAssignment assignment = findAssignmentAndVerifyChild(assignmentId, childId);

        if (assignment.getStatus() != TaskStatus.CREATED) {
            log.warn("Попытка сдать задание {} в статусе {}", assignmentId, assignment.getStatus());
            throw new ValidationException("Задание уже было сдано или проверено");
        }

        if (assignment.getDueDate() != null && assignment.getDueDate().isBefore(Instant.now())) {
            log.warn("Попытка сдать просроченное задание {}", assignmentId);
            throw new ValidationException("Дедлайн задания истёк");
        }

        assignment.setStatus(TaskStatus.SUBMITTED);
        assignment.setChildComment(request.comment());
        assignment.setSubmittedAt(Instant.now());

        // Вводное задание: авто-одобряем и разблокируем никнейм
        if (assignment.isIntroTask()) {
            TaskTemplate template = assignment.getTemplate();
            int expToGrant = template.getExpReward();
            int coinsToGrant = template.getCoinsReward();
            assignment.setStatus(TaskStatus.APPROVED);
            assignment.setReviewedAt(Instant.now());
            assignment.setExpEarned(expToGrant);
            assignment.setCoinsEarned(coinsToGrant);
            assignment = assignmentRepository.save(assignment);
            rewardService.grantReward(childId, expToGrant, coinsToGrant, false);
            try {
                userServiceClient.unlockNickname(childId);
                log.info("Никнейм ребёнка {} разблокирован после вводного задания", childId);
            } catch (Exception e) {
                log.error("Ошибка разблокировки никнейма для ребёнка {}: {}", childId, e.getMessage());
            }
            log.info("Вводное задание {} авто-одобрено: {} EXP, {} коинов", assignmentId, expToGrant, coinsToGrant);
            return assignmentMapper.toDto(assignment);
        }

        assignment = assignmentRepository.save(assignment);
        log.info("Задание {} сдано на проверку", assignmentId);

        return assignmentMapper.toDto(assignment);
    }

    @Override
    @Transactional
    @LogMethod("assignment-approve")
    public TaskAssignmentResponseDto approve(UUID assignmentId, UUID parentId, TaskReviewRequest request) {
        log.info("Одобрение задания {} родителем {}", assignmentId, parentId);

        TaskAssignment assignment = findAssignmentAndVerifyParent(assignmentId, parentId);

        if (assignment.getStatus() != TaskStatus.SUBMITTED) {
            log.warn("Попытка одобрить задание {} в статусе {}", assignmentId, assignment.getStatus());
            throw new ValidationException("Задание не находится на проверке");
        }

        // Определяем награду
        TaskTemplate template = assignment.getTemplate();
        int expToGrant = request.expReward() != null ? request.expReward() : template.getExpReward();
        int coinsToGrant = request.coinsReward() != null ? request.coinsReward() : template.getCoinsReward();

        // Обновляем статус
        assignment.setStatus(TaskStatus.APPROVED);
        assignment.setParentComment(request.comment());
        assignment.setReviewedAt(Instant.now());
        assignment.setExpEarned(expToGrant);
        assignment.setCoinsEarned(coinsToGrant);

        assignment = assignmentRepository.save(assignment);

        // Проверяем, есть ли ещё незавершённые важные задания у ребёнка
        List<TaskStatus> incompleteStatuses = List.of(TaskStatus.CREATED, TaskStatus.SUBMITTED, TaskStatus.EXPIRED);
        boolean hasIncompleteImportant = assignmentRepository.existsByChildIdAndImportantTrueAndStatusIn(
                assignment.getChildId(), incompleteStatuses
        );

        // Начисляем награду (с ограничением EXP, если есть незавершённые важные задания)
        rewardService.grantReward(assignment.getChildId(), expToGrant, coinsToGrant, hasIncompleteImportant);

        log.info("Задание {} одобрено, начислено {} EXP и {} коинов",
                assignmentId, expToGrant, coinsToGrant);

        return assignmentMapper.toDto(assignment);
    }

    @Override
    @Transactional
    @LogMethod("assignment-reject")
    public TaskAssignmentResponseDto reject(UUID assignmentId, UUID parentId, TaskReviewRequest request) {
        log.info("Отклонение задания {} родителем {}", assignmentId, parentId);

        TaskAssignment assignment = findAssignmentAndVerifyParent(assignmentId, parentId);

        if (assignment.getStatus() != TaskStatus.SUBMITTED) {
            log.warn("Попытка отклонить задание {} в статусе {}", assignmentId, assignment.getStatus());
            throw new ValidationException("Задание не находится на проверке");
        }

        assignment.setStatus(TaskStatus.REJECTED);
        assignment.setParentComment(request.comment());
        assignment.setReviewedAt(Instant.now());

        assignment = assignmentRepository.save(assignment);
        log.info("Задание {} отклонено", assignmentId);

        return assignmentMapper.toDto(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("assignment-get-expired-by-parent")
    public List<TaskAssignmentResponseDto> getExpiredByParent(UUID parentId) {
        log.debug("Получение просроченных заданий для родителя: {}", parentId);

        return assignmentRepository.findOverdueByParent(parentId, Instant.now())
                .stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    @LogMethod("assignment-extend-deadline")
    public TaskAssignmentResponseDto extendDeadline(UUID assignmentId, UUID parentId, Instant newDueDate) {
        log.info("Продление дедлайна задания {} родителем {}", assignmentId, parentId);

        TaskAssignment assignment = findAssignmentAndVerifyParent(assignmentId, parentId);

        if (assignment.getStatus() != TaskStatus.EXPIRED
                && !(assignment.getStatus() == TaskStatus.CREATED
                     && assignment.getDueDate() != null
                     && assignment.getDueDate().isBefore(Instant.now()))) {
            throw new ValidationException("Продлить дедлайн можно только для просроченных заданий");
        }

        assignment.setDueDate(newDueDate);
        assignment.setStatus(TaskStatus.CREATED);
        assignment.setReassigned(true);
        assignment = assignmentRepository.save(assignment);

        log.info("Дедлайн задания {} продлён до {}", assignmentId, newDueDate);
        return assignmentMapper.toDto(assignment);
    }

    /**
     * Найти назначение и проверить, что оно принадлежит указанному ребёнку.
     */
    private TaskAssignment findAssignmentAndVerifyChild(UUID assignmentId, UUID childId) {
        TaskAssignment assignment = assignmentRepository.findByIdWithTemplate(assignmentId)
                .orElseThrow(() -> {
                    log.error("Назначение {} не найдено", assignmentId);
                    return new ResourceNotFoundException("Назначение не найдено");
                });

        if (!assignment.getChildId().equals(childId)) {
            log.warn("Ребёнок {} пытается получить доступ к чужому заданию {}", childId, assignmentId);
            throw new UnauthorizedException("Это не ваше задание");
        }

        return assignment;
    }

    /**
     * Найти назначение и проверить, что шаблон принадлежит указанному родителю.
     */
    private TaskAssignment findAssignmentAndVerifyParent(UUID assignmentId, UUID parentId) {
        TaskAssignment assignment = assignmentRepository.findByIdWithTemplate(assignmentId)
                .orElseThrow(() -> {
                    log.error("Назначение {} не найдено", assignmentId);
                    return new ResourceNotFoundException("Назначение не найдено");
                });

        if (!assignment.getTemplate().getParentId().equals(parentId)) {
            log.warn("Родитель {} пытается получить доступ к чужому заданию {}", parentId, assignmentId);
            throw new UnauthorizedException("Вы не можете управлять этим заданием");
        }

        return assignment;
    }

    @Override
    @Transactional
    @LogMethod("assignment-delete")
    public void deleteAssignment(UUID assignmentId, UUID parentId) {
        log.info("Удаление назначения {} родителем {}", assignmentId, parentId);
        TaskAssignment assignment = findAssignmentAndVerifyParent(assignmentId, parentId);
        if (assignment.getStatus() == TaskStatus.APPROVED) {
            throw new ValidationException("Нельзя удалить выполненное задание");
        }
        assignmentRepository.delete(assignment);
        log.info("Назначение {} удалено", assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignmentResponseDto> getAllByParent(UUID parentId) {
        log.info("Получение всех назначений родителя: {}", parentId);
        return assignmentRepository.findAllByParentIdWithTemplate(parentId, Pageable.unpaged())
                .map(assignmentMapper::toDto)
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignmentResponseDto> getAllByChild(UUID childId) {
        log.info("Получение всех назначений ребёнка: {}", childId);
        return assignmentRepository.findAllByChildId(childId).stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    @LogMethod("assignment-create-intro")
    public TaskAssignmentResponseDto createIntroTask(UUID childId) {
        log.info("Создание вводного задания для ребёнка: {}", childId);

        // Проверяем, нет ли уже вводного задания
        boolean alreadyExists = assignmentRepository.findAllByChildId(childId).stream()
                .anyMatch(TaskAssignment::isIntroTask);
        if (alreadyExists) {
            log.info("Вводное задание для ребёнка {} уже существует", childId);
            return assignmentRepository.findAllByChildId(childId).stream()
                    .filter(TaskAssignment::isIntroTask)
                    .map(assignmentMapper::toDto)
                    .findFirst()
                    .orElseThrow();
        }

        TaskTemplate introTemplate = templateService.getOrCreateIntroTemplate();

        TaskAssignment assignment = TaskAssignment.builder()
                .template(introTemplate)
                .childId(childId)
                .status(TaskStatus.CREATED)
                .introTask(true)
                .build();

        assignment = assignmentRepository.save(assignment);
        log.info("Вводное задание {} создано для ребёнка {}", assignment.getId(), childId);
        return assignmentMapper.toDto(assignment);
    }
}
