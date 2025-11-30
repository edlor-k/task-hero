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
}
