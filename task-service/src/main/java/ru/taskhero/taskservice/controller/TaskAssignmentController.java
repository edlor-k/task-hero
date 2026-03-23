package ru.taskhero.taskservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.taskhero.common.exception.ExceptionBody;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.dto.TaskAssignRequest;
import ru.taskhero.taskservice.dto.TaskAssignmentResponseDto;
import ru.taskhero.taskservice.dto.TaskReviewRequest;
import ru.taskhero.taskservice.dto.TaskSubmitRequest;
import ru.taskhero.taskservice.service.TaskAssignmentService;
import ru.taskhero.taskservice.util.SecurityUtils;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер для управления назначениями заданий.
 * Поддерживает операции для родителей (назначение, проверка) и детей (просмотр, сдача).
 */
@Slf4j
@RestController
@RequestMapping("/assignments")
@RequiredArgsConstructor
@Tag(name = "Task Assignments", description = "Управление назначениями заданий")
@SecurityRequirement(name = "Bearer Authentication")
public class TaskAssignmentController {

    private final TaskAssignmentService assignmentService;

    // ==================== PARENT ENDPOINTS ====================

    /**
     * Назначить задание ребёнку.
     *
     * @param request данные для назначения
     * @return созданное назначение
     */
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Назначить задание ребёнку",
            description = "Родитель назначает задание на основе шаблона своему ребёнку"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Задание назначено",
                    content = @Content(schema = @Schema(implementation = TaskAssignmentResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные или задание уже назначено",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Шаблон не принадлежит родителю",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Шаблон не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<TaskAssignmentResponseDto> assign(@Valid @RequestBody TaskAssignRequest request) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос на назначение задания от родителя: {}", parentId);

        TaskAssignmentResponseDto response = assignmentService.assign(parentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получить все назначения родителя с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница назначений
     */
    @GetMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Получить все назначения",
            description = "Возвращает все назначения заданий родителя с пагинацией"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список назначений получен"
            )
    })
    public ResponseEntity<Page<TaskAssignmentResponseDto>> getAllByParent(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос списка назначений родителя: {}", parentId);

        Page<TaskAssignmentResponseDto> response = assignmentService.getByParent(parentId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить назначения на проверку.
     *
     * @return список назначений со статусом SUBMITTED
     */
    @GetMapping("/pending-review")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Получить назначения на проверку",
            description = "Возвращает все задания, ожидающие проверки родителем"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заданий на проверку получен"
            )
    })
    public ResponseEntity<List<TaskAssignmentResponseDto>> getPendingReview() {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос заданий на проверку от родителя: {}", parentId);

        List<TaskAssignmentResponseDto> response = assignmentService.getPendingReviewByParent(parentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить назначения конкретного ребёнка (для родителя).
     *
     * @param childId ID ребёнка
     * @param status  фильтр по статусу (опционально)
     * @return список назначений
     */
    @GetMapping("/child/{childId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Получить назначения ребёнка",
            description = "Родитель получает список назначений своего ребёнка"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список назначений получен"
            )
    })
    public ResponseEntity<List<TaskAssignmentResponseDto>> getByChild(
            @PathVariable UUID childId,
            @RequestParam(required = false) TaskStatus status
    ) {
        log.info("Запрос назначений ребёнка: {}, статус: {}", childId, status);

        List<TaskAssignmentResponseDto> response;
        if (status != null) {
            response = assignmentService.getByChildAndStatus(childId, status);
        } else {
            response = assignmentService.getByChild(childId);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Одобрить выполненное задание.
     *
     * @param id      ID назначения
     * @param request данные при проверке
     * @return обновленное назначение
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Одобрить задание",
            description = "Родитель одобряет выполненное задание и начисляет награду"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Задание одобрено",
                    content = @Content(schema = @Schema(implementation = TaskAssignmentResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Задание не находится на проверке",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Задание не принадлежит родителю",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Назначение не найдено",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<TaskAssignmentResponseDto> approve(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) TaskReviewRequest request
    ) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос на одобрение задания {} от родителя: {}", id, parentId);

        TaskReviewRequest reviewRequest = request != null ? request : new TaskReviewRequest(null, null, null);
        TaskAssignmentResponseDto response = assignmentService.approve(id, parentId, reviewRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Отклонить выполненное задание.
     *
     * @param id      ID назначения
     * @param request данные при проверке
     * @return обновленное назначение
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Отклонить задание",
            description = "Родитель отклоняет выполненное задание"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Задание отклонено",
                    content = @Content(schema = @Schema(implementation = TaskAssignmentResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Задание не находится на проверке",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Задание не принадлежит родителю",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Назначение не найдено",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<TaskAssignmentResponseDto> reject(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) TaskReviewRequest request
    ) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос на отклонение задания {} от родителя: {}", id, parentId);

        TaskReviewRequest reviewRequest = request != null ? request : new TaskReviewRequest(null, null, null);
        TaskAssignmentResponseDto response = assignmentService.reject(id, parentId, reviewRequest);
        return ResponseEntity.ok(response);
    }

    // ==================== CHILD ENDPOINTS ====================

    /**
     * Получить свои задания (для ребёнка).
     *
     * @param status фильтр по статусу (опционально)
     * @return список назначений
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(
            summary = "Мои задания",
            description = "Ребёнок получает список своих назначений"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заданий получен"
            )
    })
    public ResponseEntity<List<TaskAssignmentResponseDto>> getMyAssignments(
            @RequestParam(required = false) TaskStatus status
    ) {
        UUID childId = SecurityUtils.getCurrentUserId();
        log.info("Запрос своих заданий от ребёнка: {}, статус: {}", childId, status);

        List<TaskAssignmentResponseDto> response;
        if (status != null) {
            response = assignmentService.getByChildAndStatus(childId, status);
        } else {
            response = assignmentService.getByChild(childId);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Получить активные задания (для ребёнка).
     *
     * @return список активных назначений (CREATED и SUBMITTED)
     */
    @GetMapping("/my/active")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(
            summary = "Мои активные задания",
            description = "Ребёнок получает список своих активных заданий"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список активных заданий получен"
            )
    })
    public ResponseEntity<List<TaskAssignmentResponseDto>> getMyActiveAssignments() {
        UUID childId = SecurityUtils.getCurrentUserId();
        log.info("Запрос активных заданий от ребёнка: {}", childId);

        List<TaskAssignmentResponseDto> response = assignmentService.getActiveByChild(childId);
        return ResponseEntity.ok(response);
    }

    /**
     * Сдать задание на проверку.
     *
     * @param id      ID назначения
     * @param request данные при сдаче
     * @return обновленное назначение
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(
            summary = "Сдать задание",
            description = "Ребёнок сдает выполненное задание на проверку родителю"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Задание сдано на проверку",
                    content = @Content(schema = @Schema(implementation = TaskAssignmentResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Задание уже было сдано",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Это не ваше задание",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Назначение не найдено",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<TaskAssignmentResponseDto> submit(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) TaskSubmitRequest request
    ) {
        UUID childId = SecurityUtils.getCurrentUserId();
        log.info("Запрос на сдачу задания {} от ребёнка: {}", id, childId);

        TaskSubmitRequest submitRequest = request != null ? request : new TaskSubmitRequest(null);
        TaskAssignmentResponseDto response = assignmentService.submit(id, childId, submitRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить назначение по ID.
     *
     * @param id ID назначения
     * @return назначение
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT', 'CHILD')")
    @Operation(
            summary = "Получить назначение по ID",
            description = "Возвращает детальную информацию о назначении"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Назначение найдено",
                    content = @Content(schema = @Schema(implementation = TaskAssignmentResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Назначение не найдено",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<TaskAssignmentResponseDto> getById(@PathVariable UUID id) {
        log.info("Запрос назначения по ID: {}", id);

        TaskAssignmentResponseDto response = assignmentService.getById(id);
        return ResponseEntity.ok(response);
    }

    // ==================== EXPIRED TASKS ====================

    @GetMapping("/expired")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Получить просроченные задания", description = "Задания с истёкшим дедлайном")
    public ResponseEntity<List<TaskAssignmentResponseDto>> getExpired() {
        UUID parentId = SecurityUtils.getCurrentUserId();
        List<TaskAssignmentResponseDto> response = assignmentService.getExpiredByParent(parentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/extend-deadline")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Продлить дедлайн", description = "Переназначить просроченное задание с новым дедлайном")
    public ResponseEntity<TaskAssignmentResponseDto> extendDeadline(
            @PathVariable UUID id,
            @RequestParam String newDueDate
    ) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        TaskAssignmentResponseDto response = assignmentService.extendDeadline(
                id, parentId, java.time.LocalDate.parse(newDueDate));
        return ResponseEntity.ok(response);
    }
}
