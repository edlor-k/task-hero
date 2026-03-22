package ru.taskhero.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.taskhero.common.exception.ExceptionBody;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.*;
import ru.taskhero.userservice.entity.AuditAction;
import ru.taskhero.userservice.service.AdminService;
import ru.taskhero.userservice.service.AuditService;
import ru.taskhero.userservice.service.ChildService;
import ru.taskhero.userservice.service.ParentService;
import ru.taskhero.userservice.service.UserService;
import ru.taskhero.userservice.util.SecurityUtils;

import java.util.UUID;

/**
 * Контроллер для административных операций.
 * Все endpoints доступны только пользователям с ролью ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "API для администраторов системы")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final UserService userService;
    private final ParentService parentService;
    private final ChildService childService;
    private final AdminService adminService;
    private final AuditService auditService;

    /**
     * Получить список всех пользователей с пагинацией и фильтрами.
     */
    @GetMapping("/users")
    @Operation(
            summary = "Получить список всех пользователей",
            description = "Возвращает пагинированный список пользователей с возможностью фильтрации по роли и активности"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список пользователей получен",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Фильтр по роли") @RequestParam(required = false) Role role,
            @Parameter(description = "Фильтр по активности") @RequestParam(required = false) Boolean active
    ) {
        log.info("Админ: запрос списка пользователей, role={}, active={}, page={}", role, active, pageable.getPageNumber());
        Page<UserResponseDto> users = userService.getAllUsers(pageable, role, active);
        return ResponseEntity.ok(users);
    }

    /**
     * Поиск пользователей по email.
     */
    @GetMapping("/users/search")
    @Operation(
            summary = "Поиск пользователей по email",
            description = "Выполняет поиск пользователей по частичному совпадению email"
    )
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
            @Parameter(description = "Строка поиска") @RequestParam String q,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Админ: поиск пользователей по email: {}", q);
        Page<UserResponseDto> users = userService.searchByEmail(q, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Переключить активность пользователя (блокировка/разблокировка).
     */
    @PatchMapping("/users/{id}/toggle-active")
    @Operation(
            summary = "Заблокировать/разблокировать пользователя",
            description = "Переключает статус активности пользователя (is_active)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус активности изменен",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<UserResponseDto> toggleUserActive(@PathVariable UUID id) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        if (currentAdminId.equals(id)) {
            throw new ValidationException("Нельзя заблокировать самого себя");
        }
        log.info("Админ: переключение активности пользователя: {}", id);
        UserResponseDto user = userService.toggleActive(id);
        UserResponseDto admin = userService.getById(currentAdminId);
        AuditAction action = user.isActive() ? AuditAction.USER_UNBLOCKED : AuditAction.USER_BLOCKED;
        auditService.log(currentAdminId, admin.email(), action, "USER", id, null);
        return ResponseEntity.ok(user);
    }

    /**
     * Изменить роль пользователя.
     */
    @PatchMapping("/users/{id}/role")
    @Operation(
            summary = "Изменить роль пользователя",
            description = "Обновляет роль пользователя в системе"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Роль изменена",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<UserResponseDto> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        if (currentAdminId.equals(id)) {
            throw new ValidationException("Нельзя изменить роль самому себе");
        }
        log.info("Админ: изменение роли пользователя {}: новая роль={}", id, request.newRole());
        UserResponseDto user = userService.updateRole(id, request.newRole());
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.USER_ROLE_CHANGED, "USER", id,
                "Новая роль: " + request.newRole());
        return ResponseEntity.ok(user);
    }

    /**
     * Удалить пользователя (soft delete).
     */
    @DeleteMapping("/users/{id}")
    @Operation(
            summary = "Удалить пользователя",
            description = "Выполняет soft delete - устанавливает is_active=false"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь удален"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        if (currentAdminId.equals(id)) {
            throw new ValidationException("Нельзя удалить самого себя");
        }
        log.info("Админ: удаление пользователя: {}", id);
        UserResponseDto admin = userService.getById(currentAdminId);
        userService.deleteUser(id);
        auditService.log(currentAdminId, admin.email(), AuditAction.USER_DELETED, "USER", id, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получить список всех родителей с информацией о детях.
     */
    @GetMapping("/parents")
    @Operation(
            summary = "Получить список всех родителей",
            description = "Возвращает пагинированный список родителей с информацией о их детях"
    )
    public ResponseEntity<Page<ParentWithChildrenDto>> getAllParents(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Админ: запрос списка родителей, page={}", pageable.getPageNumber());
        Page<ParentWithChildrenDto> parents = parentService.getAllParents(pageable);
        return ResponseEntity.ok(parents);
    }

    /**
     * Получить детальную информацию о родителе.
     */
    @GetMapping("/parents/{id}")
    @Operation(
            summary = "Получить детальную информацию о родителе",
            description = "Возвращает полную информацию о родителе, включая всех его детей"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация получена",
                    content = @Content(schema = @Schema(implementation = ParentDetailDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Родитель не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<ParentDetailDto> getParentDetail(@PathVariable UUID id) {
        log.info("Админ: запрос детальной информации о родителе: {}", id);
        ParentDetailDto parent = parentService.getDetailById(id);
        return ResponseEntity.ok(parent);
    }

    /**
     * Обновить профиль родителя.
     */
    @PutMapping("/parents/{id}")
    @Operation(
            summary = "Обновить профиль родителя",
            description = "Обновляет имя и фамилию родителя"
    )
    public ResponseEntity<ParentResponseDto> updateParent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParentRequest request
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: обновление профиля родителя: {}", id);
        ParentResponseDto parent = parentService.updateParent(id, request);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.PARENT_UPDATED, "PARENT", id, null);
        return ResponseEntity.ok(parent);
    }

    /**
     * Получить список всех детей в системе.
     */
    @GetMapping("/children")
    @Operation(
            summary = "Получить список всех детей",
            description = "Возвращает пагинированный список всех детей в системе"
    )
    public ResponseEntity<Page<ChildResponseDto>> getAllChildren(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Админ: запрос списка всех детей, page={}", pageable.getPageNumber());
        Page<ChildResponseDto> children = childService.getAllChildren(pageable);
        return ResponseEntity.ok(children);
    }

    /**
     * Получить детальную информацию о ребенке.
     */
    @GetMapping("/children/{id}")
    @Operation(
            summary = "Получить детальную информацию о ребенке",
            description = "Возвращает полную информацию о ребенке, включая информацию о родителе"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация получена",
                    content = @Content(schema = @Schema(implementation = ChildDetailDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ребенок не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<ChildDetailDto> getChildDetail(@PathVariable UUID id) {
        log.info("Админ: запрос детальной информации о ребенке: {}", id);
        ChildDetailDto child = childService.getDetailById(id);
        return ResponseEntity.ok(child);
    }

    /**
     * Обновить данные ребенка.
     */
    @PutMapping("/children/{id}")
    @Operation(
            summary = "Обновить данные ребенка",
            description = "Обновляет имя, возраст, опыт, монеты и уровень ребенка"
    )
    public ResponseEntity<ChildResponseDto> updateChild(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateChildRequest request
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: обновление данных ребенка: {}", id);
        ChildResponseDto child = childService.updateChild(id, request);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.CHILD_UPDATED, "CHILD", id, null);
        return ResponseEntity.ok(child);
    }

    /**
     * Удалить ребенка.
     */
    @DeleteMapping("/children/{id}")
    @Operation(
            summary = "Удалить ребенка",
            description = "Полностью удаляет ребенка из системы"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ребенок удален"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ребенок не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<Void> deleteChild(@PathVariable UUID id) {
        log.info("Админ: удаление ребенка: {}", id);
        childService.deleteChild(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получить общую статистику системы.
     */
    @GetMapping("/statistics")
    @Operation(
            summary = "Получить статистику системы",
            description = "Возвращает общую статистику: количество пользователей, родителей, детей и т.д."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Статистика получена",
                    content = @Content(schema = @Schema(implementation = SystemStatisticsDto.class))
            )
    })
    public ResponseEntity<SystemStatisticsDto> getStatistics() {
        log.info("Админ: запрос статистики системы");
        SystemStatisticsDto statistics = adminService.getSystemStatistics();
        return ResponseEntity.ok(statistics);
    }

    // ==================== Audit Log ====================

    /**
     * Получить журнал аудита.
     */
    @GetMapping("/audit")
    @Operation(
            summary = "Получить журнал аудита",
            description = "Возвращает пагинированный список действий администраторов"
    )
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Админ: запрос журнала аудита, page={}", pageable.getPageNumber());
        Page<AuditLogDto> logs = auditService.getAuditLogs(pageable);
        return ResponseEntity.ok(logs);
    }
}
