package ru.taskhero.userservice.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.taskhero.common.exception.ExceptionBody;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.CharacterType;
import ru.taskhero.userservice.dto.ChildCreateRequestDto;
import ru.taskhero.userservice.dto.ChildResponseDto;
import ru.taskhero.userservice.dto.ParentResponseDto;
import ru.taskhero.userservice.dto.UpdateChildRequest;
import ru.taskhero.userservice.service.ChildService;
import ru.taskhero.userservice.service.ParentService;
import ru.taskhero.userservice.util.SecurityUtils;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер для управления детьми.
 * Все endpoints требуют аутентификации (JWT токен) и роли PARENT.
 */
@Slf4j
@RestController
@RequestMapping("/children")
@RequiredArgsConstructor
@Tag(name = "Children", description = "Управление детьми родителя")
@SecurityRequirement(name = "Bearer Authentication")
public class ChildController {

    private final ChildService childService;
    private final ParentService parentService;

    /**
     * Добавить нового ребенка к текущему родителю.
     *
     * @param request данные для создания ребенка
     * @return DTO созданного ребенка
     */
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Добавить ребенка",
            description = "Создает нового ребенка и привязывает к текущему родителю. Генерирует уникальный loginToken."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Ребенок успешно создан",
                    content = @Content(schema = @Schema(implementation = ChildResponseDto.class))
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Профиль родителя не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<ChildResponseDto> createChild(@Valid @RequestBody ChildCreateRequestDto request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Запрос на создание ребенка от пользователя: {}", currentUserId);

        // Получаем ID родителя по userId
        ParentResponseDto parent = parentService.getByUserId(currentUserId);

        ChildResponseDto response = childService.createChild(parent.id(), request);
        log.info("Ребенок создан с loginToken: {}", response.loginToken());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получить список всех детей текущего родителя.
     *
     * @return список DTO детей
     */
    @GetMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Получить список детей",
            description = "Возвращает всех детей текущего родителя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список детей получен",
                    content = @Content(schema = @Schema(implementation = ChildResponseDto.class))
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Профиль родителя не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<List<ChildResponseDto>> getChildren() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Запрос списка детей для пользователя: {}", currentUserId);

        // Получаем ID родителя по userId
        ParentResponseDto parent = parentService.getByUserId(currentUserId);

        List<ChildResponseDto> response = childService.getChildrenByParent(parent.id());
        log.info("Найдено детей: {}", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Получить профиль текущего ребёнка (по JWT).
     *
     * @return DTO ребёнка с информацией об опыте
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(
            summary = "Получить свой профиль (ребёнок)",
            description = "Возвращает профиль текущего авторизованного ребёнка"
    )
    public ResponseEntity<ChildResponseDto> getMyProfile() {
        UUID childId = SecurityUtils.getCurrentUserId();
        log.info("Запрос профиля ребёнка: {}", childId);
        ChildResponseDto response = childService.getById(childId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить информацию о ребенке по ID.
     *
     * @param id ID ребенка
     * @return DTO ребенка
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @Operation(
            summary = "Получить ребенка по ID",
            description = "Возвращает информацию о ребенке по его ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация о ребенке получена",
                    content = @Content(schema = @Schema(implementation = ChildResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ребенок не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<ChildResponseDto> getChildById(@PathVariable UUID id) {
        log.info("Запрос ребенка по ID: {}", id);
        ChildResponseDto response = childService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить информацию о ребенке по loginToken.
     * Этот endpoint может использоваться для проверки токена перед входом.
     *
     * @param token loginToken ребенка
     * @return DTO ребенка
     */
    @GetMapping("/by-token/{token}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Получить ребенка по loginToken",
            description = "Возвращает информацию о ребенке по его уникальному токену входа"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация о ребенке получена",
                    content = @Content(schema = @Schema(implementation = ChildResponseDto.class))
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ребенок с таким токеном не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<ChildResponseDto> getChildByToken(@PathVariable String token) {
        log.info("Запрос ребенка по токену");
        ChildResponseDto response = childService.getByLoginToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновить данные ребенка (родителем).
     *
     * @param id      ID ребенка
     * @param request данные для обновления
     * @return обновленное DTO ребенка
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Обновить данные ребенка",
            description = "Родитель может обновить имя и возраст своего ребенка"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Данные ребенка обновлены",
                    content = @Content(schema = @Schema(implementation = ChildResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Ребенок не принадлежит текущему родителю",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ребенок не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<ChildResponseDto> updateChild(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateChildRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Обновление данных ребенка {} родителем: {}", id, currentUserId);

        // Получаем ID родителя
        ParentResponseDto parent = parentService.getByUserId(currentUserId);

        // Проверяем, принадлежит ли ребенок этому родителю
        if (!childService.isChildBelongsToParent(id, parent.id())) {
            log.warn("Родитель {} пытается обновить чужого ребенка {}", parent.id(), id);
            throw new ValidationException("Вы не можете обновлять данные этого ребенка");
        }

        ChildResponseDto response = childService.updateChild(id, request);
        log.info("Данные ребенка {} обновлены", id);

        return ResponseEntity.ok(response);
    }

    /**
     * Удалить ребенка (родителем).
     *
     * @param id ID ребенка
     * @return статус 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Удалить ребенка",
            description = "Родитель может удалить своего ребенка из системы"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Ребенок удален"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Ребенок не принадлежит текущему родителю",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ребенок не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<Void> deleteChild(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Удаление ребенка {} родителем: {}", id, currentUserId);

        // Получаем ID родителя
        ParentResponseDto parent = parentService.getByUserId(currentUserId);

        // Проверяем, принадлежит ли ребенок этому родителю
        if (!childService.isChildBelongsToParent(id, parent.id())) {
            log.warn("Родитель {} пытается удалить чужого ребенка {}", parent.id(), id);
            throw new ValidationException("Вы не можете удалить этого ребенка");
        }

        childService.deleteChild(id);
        log.info("Ребенок {} удален", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Начислить награду ребёнку.
     * Используется task-service для начисления EXP и коинов.
     *
     * @param childId ID ребёнка
     * @param request данные о награде
     * @return обновленные данные ребёнка
     */
    @PutMapping("/{childId}/reward")
    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @Operation(
            summary = "Начислить награду ребёнку",
            description = "Начисляет EXP и коины ребёнку. Автоматически пересчитывает уровень."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Награда начислена",
                    content = @Content(schema = @Schema(implementation = ChildResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ребёнок не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<ChildResponseDto> addReward(
            @PathVariable UUID childId,
            @Valid @RequestBody ru.taskhero.userservice.dto.RewardRequest request
    ) {
        log.info("Начисление награды ребёнку {}: {} EXP, {} коинов", childId, request.exp(), request.coins());

        ChildResponseDto response = childService.addReward(childId, request.exp(), request.coins());
        return ResponseEntity.ok(response);
    }

    /**
     * Выбрать персонажа для ребёнка (при первом входе).
     *
     * @param characterType тип персонажа
     * @return обновленные данные ребёнка
     */
    @PostMapping("/me/character")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(
            summary = "Выбрать персонажа",
            description = "Ребёнок выбирает своего персонажа при первом входе"
    )
    public ResponseEntity<ChildResponseDto> selectCharacter(
            @RequestParam CharacterType characterType
    ) {
        UUID childId = SecurityUtils.getCurrentUserId();
        log.info("Выбор персонажа {} ребёнком {}", characterType, childId);

        ChildResponseDto response = childService.selectCharacter(childId, characterType);
        return ResponseEntity.ok(response);
    }

    /**
     * Проверить, принадлежит ли ребёнок родителю.
     * Используется task-service для проверки доступа.
     *
     * @param childId  ID ребёнка
     * @param parentId ID родителя
     * @return true если ребёнок принадлежит родителю
     */
    @GetMapping("/{childId}/belongs-to/{parentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @Operation(
            summary = "Проверить принадлежность ребёнка",
            description = "Проверяет, принадлежит ли ребёнок указанному родителю"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Результат проверки"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ребёнок не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<Boolean> isChildBelongsToParent(
            @PathVariable UUID childId,
            @PathVariable UUID parentId
    ) {
        log.info("Проверка принадлежности ребёнка {} родителю {}", childId, parentId);

        boolean belongs = childService.isChildBelongsToParent(childId, parentId);
        return ResponseEntity.ok(belongs);
    }
}
