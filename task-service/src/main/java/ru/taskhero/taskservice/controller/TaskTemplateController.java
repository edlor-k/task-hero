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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.taskhero.common.model.enums.TaskCategory;
import ru.taskhero.common.exception.ExceptionBody;
import ru.taskhero.taskservice.dto.TaskTemplateCreateRequest;
import ru.taskhero.taskservice.dto.TaskTemplateResponseDto;
import ru.taskhero.taskservice.dto.TaskTemplateUpdateRequest;
import ru.taskhero.taskservice.service.TaskTemplateService;
import ru.taskhero.taskservice.util.SecurityUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Контроллер для управления шаблонами заданий.
 * Все endpoints требуют аутентификации и роли PARENT.
 */
@Slf4j
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
@Tag(name = "Task Templates", description = "Управление шаблонами заданий")
@SecurityRequirement(name = "Bearer Authentication")
public class TaskTemplateController {

    private final TaskTemplateService templateService;

    /**
     * Создать новый шаблон задания.
     *
     * @param request данные для создания шаблона
     * @return созданный шаблон
     */
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Создать шаблон задания",
            description = "Создает новый шаблон задания для текущего родителя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Шаблон успешно создан",
                    content = @Content(schema = @Schema(implementation = TaskTemplateResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав (требуется роль PARENT)",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<TaskTemplateResponseDto> create(@Valid @RequestBody TaskTemplateCreateRequest request) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос на создание шаблона от родителя: {}", parentId);

        TaskTemplateResponseDto response = templateService.create(parentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получить список шаблонов текущего родителя с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница шаблонов
     */
    @GetMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Получить список шаблонов",
            description = "Возвращает все шаблоны текущего родителя с пагинацией"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список шаблонов получен"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<Page<TaskTemplateResponseDto>> getAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос списка шаблонов родителя: {}", parentId);

        Page<TaskTemplateResponseDto> response = templateService.getByParent(parentId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить активные шаблоны текущего родителя.
     *
     * @return список активных шаблонов
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Получить активные шаблоны",
            description = "Возвращает все активные шаблоны текущего родителя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список активных шаблонов получен"
            )
    })
    public ResponseEntity<List<TaskTemplateResponseDto>> getActive() {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос активных шаблонов родителя: {}", parentId);

        List<TaskTemplateResponseDto> response = templateService.getActiveByParent(parentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить шаблон по ID.
     *
     * @param id ID шаблона
     * @return шаблон
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Получить шаблон по ID",
            description = "Возвращает шаблон по его идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Шаблон найден",
                    content = @Content(schema = @Schema(implementation = TaskTemplateResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Шаблон не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<TaskTemplateResponseDto> getById(@PathVariable UUID id) {
        log.info("Запрос шаблона по ID: {}", id);

        TaskTemplateResponseDto response = templateService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновить шаблон задания.
     *
     * @param id      ID шаблона
     * @param request данные для обновления
     * @return обновленный шаблон
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Обновить шаблон",
            description = "Обновляет существующий шаблон задания"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Шаблон обновлен",
                    content = @Content(schema = @Schema(implementation = TaskTemplateResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Шаблон не принадлежит текущему родителю",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Шаблон не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<TaskTemplateResponseDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskTemplateUpdateRequest request
    ) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос на обновление шаблона {} от родителя: {}", id, parentId);

        TaskTemplateResponseDto response = templateService.update(id, parentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Удалить шаблон задания.
     *
     * @param id ID шаблона
     * @return статус 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Удалить шаблон",
            description = "Удаляет шаблон задания"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Шаблон удален"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Шаблон не принадлежит текущему родителю",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Шаблон не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос на удаление шаблона {} от родителя: {}", id, parentId);

        templateService.delete(id, parentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получить все шаблоны из библиотеки.
     */
    @GetMapping("/library")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Получить шаблоны библиотеки",
            description = "Возвращает все шаблоны из библиотеки готовых заданий")
    public ResponseEntity<List<TaskTemplateResponseDto>> getLibrary(
            @RequestParam(required = false) TaskCategory category
    ) {
        log.info("Запрос шаблонов библиотеки, категория: {}", category);
        List<TaskTemplateResponseDto> response;
        if (category != null) {
            response = templateService.getLibraryTemplatesByCategory(category);
        } else {
            response = templateService.getLibraryTemplates();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Скопировать шаблон из библиотеки себе.
     */
    @PostMapping("/library/{id}/copy")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Копировать шаблон из библиотеки",
            description = "Копирует шаблон из библиотеки в список шаблонов родителя")
    public ResponseEntity<TaskTemplateResponseDto> copyFromLibrary(@PathVariable UUID id) {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Копирование шаблона {} из библиотеки, родитель: {}", id, parentId);

        TaskTemplateResponseDto response = templateService.copyFromLibrary(id, parentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получить ID библиотечных шаблонов, уже скопированных текущим родителем.
     */
    @GetMapping("/library/copied-ids")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Получить ID скопированных шаблонов",
            description = "Возвращает множество ID библиотечных шаблонов, которые родитель уже копировал")
    public ResponseEntity<Set<UUID>> getCopiedLibraryIds() {
        UUID parentId = SecurityUtils.getCurrentUserId();
        log.info("Запрос ID скопированных шаблонов библиотеки, родитель: {}", parentId);

        Set<UUID> copiedIds = templateService.getCopiedLibraryTemplateIds(parentId);
        return ResponseEntity.ok(copiedIds);
    }
}
