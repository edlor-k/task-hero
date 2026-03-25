package ru.taskhero.taskservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.taskhero.taskservice.dto.TaskAssignmentResponseDto;
import ru.taskhero.taskservice.dto.TaskTemplateCreateRequest;
import ru.taskhero.taskservice.dto.TaskTemplateResponseDto;
import ru.taskhero.taskservice.dto.TaskTemplateUpdateRequest;
import ru.taskhero.taskservice.service.TaskAssignmentService;
import ru.taskhero.taskservice.service.TaskTemplateService;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер для администрирования библиотеки шаблонов.
 */
@Slf4j
@RestController
@RequestMapping("/admin/templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Templates", description = "Управление библиотекой шаблонов")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminTemplateController {

    private final TaskTemplateService templateService;
    private final TaskAssignmentService assignmentService;

    /**
     * Получить все шаблоны библиотеки.
     */
    @GetMapping("/library")
    @Operation(summary = "Получить шаблоны библиотеки")
    public ResponseEntity<List<TaskTemplateResponseDto>> getLibraryTemplates() {
        log.info("Админ: запрос шаблонов библиотеки");
        return ResponseEntity.ok(templateService.getLibraryTemplates());
    }

    /**
     * Создать шаблон в библиотеке.
     */
    @PostMapping("/library")
    @Operation(summary = "Создать шаблон в библиотеке")
    public ResponseEntity<TaskTemplateResponseDto> createLibraryTemplate(
            @Valid @RequestBody TaskTemplateCreateRequest request
    ) {
        log.info("Админ: создание шаблона библиотеки: {}", request.title());
        TaskTemplateResponseDto template = templateService.createLibraryTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    /**
     * Обновить шаблон библиотеки.
     */
    @PutMapping("/library/{id}")
    @Operation(summary = "Обновить шаблон библиотеки")
    public ResponseEntity<TaskTemplateResponseDto> updateLibraryTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody TaskTemplateUpdateRequest request
    ) {
        log.info("Админ: обновление шаблона библиотеки: {}", id);
        TaskTemplateResponseDto template = templateService.updateLibraryTemplate(id, request);
        return ResponseEntity.ok(template);
    }

    /**
     * Удалить шаблон библиотеки.
     */
    @DeleteMapping("/library/{id}")
    @Operation(summary = "Удалить шаблон библиотеки")
    public ResponseEntity<Void> deleteLibraryTemplate(@PathVariable UUID id) {
        log.info("Админ: удаление шаблона библиотеки: {}", id);
        templateService.deleteLibraryTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получить все шаблоны заданий конкретного родителя.
     */
    @GetMapping("/parent/{parentId}")
    @Operation(summary = "Получить шаблоны родителя", description = "Все шаблоны заданий, созданные родителем")
    public ResponseEntity<List<TaskTemplateResponseDto>> getParentTemplates(@PathVariable UUID parentId) {
        log.info("Админ: запрос шаблонов родителя: {}", parentId);
        return ResponseEntity.ok(templateService.getAllByParent(parentId));
    }

    /**
     * Получить все назначения заданий конкретного родителя.
     */
    @GetMapping("/parent/{parentId}/assignments")
    @Operation(summary = "Получить назначения родителя", description = "Все назначенные задания родителем")
    public ResponseEntity<List<TaskAssignmentResponseDto>> getParentAssignments(@PathVariable UUID parentId) {
        log.info("Админ: запрос назначений родителя: {}", parentId);
        return ResponseEntity.ok(assignmentService.getAllByParent(parentId));
    }

    /**
     * Получить все назначения заданий конкретного ребёнка.
     */
    @GetMapping("/child/{childId}/assignments")
    @Operation(summary = "Получить назначения ребёнка", description = "Все задания, назначенные ребёнку")
    public ResponseEntity<List<TaskAssignmentResponseDto>> getChildAssignments(@PathVariable UUID childId) {
        log.info("Админ: запрос назначений ребёнка: {}", childId);
        return ResponseEntity.ok(assignmentService.getAllByChild(childId));
    }
}
