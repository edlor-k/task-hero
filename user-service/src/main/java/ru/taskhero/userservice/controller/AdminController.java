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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.taskhero.common.exception.ExceptionBody;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.*;
import ru.taskhero.userservice.entity.AuditAction;
import ru.taskhero.userservice.service.AdminService;
import ru.taskhero.userservice.service.AuditService;
import ru.taskhero.userservice.service.AvatarOptionService;
import ru.taskhero.userservice.service.BackgroundOptionService;
import ru.taskhero.userservice.service.ChildService;
import ru.taskhero.userservice.service.ParentService;
import ru.taskhero.userservice.service.ShopService;
import ru.taskhero.userservice.service.UserService;
import ru.taskhero.userservice.util.SecurityUtils;

import java.util.List;
import java.util.Map;
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
    private final ShopService shopService;
    private final AvatarOptionService avatarOptionService;
    private final BackgroundOptionService backgroundOptionService;

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
    @PostMapping("/users/{id}/toggle-active")
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
    @PostMapping("/users/{id}/role")
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
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Поиск по имени/email") @RequestParam(required = false) String q
    ) {
        log.info("Админ: запрос списка родителей, page={}, q={}", pageable.getPageNumber(), q);
        Page<ParentWithChildrenDto> parents;
        if (q != null && !q.isBlank()) {
            parents = parentService.searchParents(q, pageable);
        } else {
            parents = parentService.getAllParents(pageable);
        }
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
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Поиск по имени") @RequestParam(required = false) String q
    ) {
        log.info("Админ: запрос списка всех детей, page={}, q={}", pageable.getPageNumber(), q);
        Page<ChildResponseDto> children;
        if (q != null && !q.isBlank()) {
            children = childService.searchChildren(q, pageable);
        } else {
            children = childService.getAllChildren(pageable);
        }
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
     * Создать запись аудита (для внешних сервисов).
     */
    @PostMapping("/audit")
    public ResponseEntity<Void> createAuditEntry(
            @RequestBody Map<String, String> request
    ) {
        UUID actorId = request.get("actorId") != null ? UUID.fromString(request.get("actorId")) : null;
        String actorEmail = request.get("actorEmail");
        AuditAction action = AuditAction.valueOf(request.get("action"));
        String targetType = request.get("targetType");
        UUID targetId = request.get("targetId") != null ? UUID.fromString(request.get("targetId")) : null;
        String details = request.get("details");
        auditService.log(actorId, actorEmail, action, targetType, targetId, details);
        return ResponseEntity.ok().build();
    }

    /**
     * Получить журнал аудита.
     */
    @GetMapping("/audit")
    @Operation(
            summary = "Получить журнал аудита",
            description = "Возвращает пагинированный список действий администраторов с фильтрацией"
    )
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Фильтр по действию") @RequestParam(required = false) AuditAction action,
            @Parameter(description = "Дата c (ISO)") @RequestParam(required = false) String dateFrom,
            @Parameter(description = "Дата по (ISO)") @RequestParam(required = false) String dateTo
    ) {
        log.info("Админ: запрос журнала аудита, page={}, action={}", pageable.getPageNumber(), action);
        java.time.Instant from = dateFrom != null ? java.time.Instant.parse(dateFrom) : null;
        java.time.Instant to = dateTo != null ? java.time.Instant.parse(dateTo) : null;
        Page<AuditLogDto> logs = auditService.getAuditLogs(pageable, action, from, to);
        return ResponseEntity.ok(logs);
    }

    // ==================== Password Reset ====================

    /**
     * Сбросить пароль пользователя.
     */
    @PostMapping("/users/{id}/password")
    @Operation(
            summary = "Сбросить пароль пользователя",
            description = "Устанавливает новый пароль для пользователя"
    )
    public ResponseEntity<Void> resetPassword(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request
    ) {
        String newPassword = request.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException("Пароль должен содержать минимум 6 символов");
        }
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: сброс пароля пользователя: {}", id);
        userService.resetPassword(id, newPassword);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.USER_PASSWORD_RESET, "USER", id, null);
        return ResponseEntity.ok().build();
    }

    // ==================== Marketplace Management ====================

    /**
     * Получить все товары маркетплейса.
     */
    @GetMapping("/marketplace")
    @Operation(summary = "Получить товары маркетплейса", description = "Список всех системных наград из маркетплейса")
    public ResponseEntity<java.util.List<ShopItemResponseDto>> getMarketplaceItems() {
        log.info("Админ: запрос товаров маркетплейса");
        return ResponseEntity.ok(shopService.getMarketplaceItems());
    }

    /**
     * Создать товар в маркетплейсе.
     */
    @PostMapping("/marketplace")
    @Operation(summary = "Создать товар в маркетплейсе")
    public ResponseEntity<ShopItemResponseDto> createMarketplaceItem(@Valid @RequestBody ShopItemCreateRequest request) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: создание товара в маркетплейсе: {}", request.title());
        ShopItemResponseDto item = shopService.createMarketplaceItem(request);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.MARKETPLACE_ITEM_CREATED,
                "MARKETPLACE", item.id(), "Товар: " + request.title());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(item);
    }

    /**
     * Обновить товар маркетплейса.
     */
    @PutMapping("/marketplace/{id}")
    @Operation(summary = "Обновить товар маркетплейса")
    public ResponseEntity<ShopItemResponseDto> updateMarketplaceItem(
            @PathVariable UUID id,
            @Valid @RequestBody ShopItemUpdateRequest request
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: обновление товара маркетплейса: {}", id);
        ShopItemResponseDto item = shopService.updateMarketplaceItem(id, request);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.MARKETPLACE_ITEM_UPDATED,
                "MARKETPLACE", id, null);
        return ResponseEntity.ok(item);
    }

    /**
     * Удалить товар маркетплейса.
     */
    @DeleteMapping("/marketplace/{id}")
    @Operation(summary = "Удалить товар маркетплейса")
    public ResponseEntity<Void> deleteMarketplaceItem(@PathVariable UUID id) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: удаление товара маркетплейса: {}", id);
        shopService.deleteMarketplaceItem(id);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.MARKETPLACE_ITEM_DELETED,
                "MARKETPLACE", id, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получить товары магазина конкретного родителя.
     */
    @GetMapping("/parents/{id}/shop-items")
    @Operation(summary = "Получить товары родителя", description = "Список наград/товаров, созданных родителем")
    public ResponseEntity<java.util.List<ShopItemResponseDto>> getParentShopItems(@PathVariable UUID id) {
        log.info("Админ: запрос товаров родителя: {}", id);
        return ResponseEntity.ok(shopService.getItemsByParent(id));
    }

    /**
     * Получить покупки детей конкретного родителя.
     */
    @GetMapping("/parents/{id}/purchases")
    @Operation(summary = "Получить покупки у родителя", description = "Все покупки детей данного родителя")
    public ResponseEntity<java.util.List<ShopPurchaseResponseDto>> getParentPurchases(@PathVariable UUID id) {
        log.info("Админ: запрос покупок родителя: {}", id);
        return ResponseEntity.ok(shopService.getAllPurchasesByParent(id));
    }

    // ==================== Avatar Gallery Management ====================

    /**
     * Получить все опции аватара (вкл. неактивные).
     */
    @GetMapping("/avatar-options")
    @Operation(summary = "Получить галерею аватаров", description = "Список всех опций аватара, включая неактивные")
    public ResponseEntity<List<AvatarOptionResponseDto>> getAvatarOptions() {
        log.info("Админ: запрос галереи аватаров");
        return ResponseEntity.ok(avatarOptionService.listAll());
    }

    /**
     * Загрузить новую картинку в галерею аватаров.
     */
    @PostMapping("/avatar-options")
    @Operation(summary = "Загрузить аватар", description = "Загружает картинку (PNG/WebP, квадратная, 256-1024px) в галерею")
    public ResponseEntity<AvatarOptionResponseDto> createAvatarOption(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String label
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: загрузка новой опции аватара");
        AvatarOptionResponseDto option = avatarOptionService.create(file, label);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.AVATAR_OPTION_CREATED,
                "AVATAR_OPTION", option.id(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(option);
    }

    /**
     * Включить/выключить видимость опции аватара в пикере ребёнка.
     */
    @PatchMapping("/avatar-options/{id}")
    @Operation(summary = "Переключить активность аватара", description = "Скрыть/показать опцию в пикере ребёнка")
    public ResponseEntity<AvatarOptionResponseDto> toggleAvatarOption(
            @PathVariable UUID id,
            @RequestParam boolean active
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: переключение активности опции аватара {}: active={}", id, active);
        AvatarOptionResponseDto option = avatarOptionService.setActive(id, active);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.AVATAR_OPTION_UPDATED,
                "AVATAR_OPTION", id, "active=" + active);
        return ResponseEntity.ok(option);
    }

    /**
     * Удалить опцию аватара из галереи.
     */
    @DeleteMapping("/avatar-options/{id}")
    @Operation(summary = "Удалить аватар", description = "Удаляет опцию и файл из хранилища (если её никто не выбрал)")
    public ResponseEntity<Void> deleteAvatarOption(@PathVariable UUID id) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: удаление опции аватара: {}", id);
        avatarOptionService.delete(id);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.AVATAR_OPTION_DELETED,
                "AVATAR_OPTION", id, null);
        return ResponseEntity.noContent().build();
    }

    // ==================== Background Gallery Management ====================

    @GetMapping("/background-options")
    @Operation(summary = "Получить галерею фонов", description = "Список всех фонов, включая неактивные")
    public ResponseEntity<List<BackgroundOptionResponseDto>> getBackgroundOptions() {
        log.info("Админ: запрос галереи фонов");
        return ResponseEntity.ok(backgroundOptionService.listAll());
    }

    @PostMapping("/background-options")
    @Operation(summary = "Загрузить фон", description = "Загружает картинку (PNG/WebP/JPEG) в галерею фонов")
    public ResponseEntity<BackgroundOptionResponseDto> createBackgroundOption(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String label
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: загрузка нового фона");
        BackgroundOptionResponseDto option = backgroundOptionService.create(file, label);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.BACKGROUND_OPTION_CREATED,
                "BACKGROUND_OPTION", option.id(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(option);
    }

    @PatchMapping("/background-options/{id}")
    @Operation(summary = "Переключить активность фона")
    public ResponseEntity<BackgroundOptionResponseDto> toggleBackgroundOption(
            @PathVariable UUID id,
            @RequestParam boolean active
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: переключение активности фона {}: active={}", id, active);
        BackgroundOptionResponseDto option = backgroundOptionService.setActive(id, active);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.BACKGROUND_OPTION_UPDATED,
                "BACKGROUND_OPTION", id, "active=" + active);
        return ResponseEntity.ok(option);
    }

    @DeleteMapping("/background-options/{id}")
    @Operation(summary = "Удалить фон", description = "Удаляет фон и файл из хранилища (если его никто не выбрал)")
    public ResponseEntity<Void> deleteBackgroundOption(@PathVariable UUID id) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: удаление фона: {}", id);
        backgroundOptionService.delete(id);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.BACKGROUND_OPTION_DELETED,
                "BACKGROUND_OPTION", id, null);
        return ResponseEntity.noContent().build();
    }

    // ==================== Admin force-change avatar / background ====================

    @PostMapping("/children/{id}/force-avatar")
    @Operation(summary = "Принудительно установить аватар ребёнку", description = "Без проверки уровня")
    public ResponseEntity<ChildResponseDto> forceSetAvatar(
            @PathVariable UUID id,
            @RequestParam UUID avatarOptionId
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: принудительная смена аватара ребёнка {}", id);
        ChildResponseDto child = childService.adminForceSetAvatar(id, avatarOptionId);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.CHILD_AVATAR_FORCED,
                "CHILD", id, "avatarOptionId=" + avatarOptionId);
        return ResponseEntity.ok(child);
    }

    @PostMapping("/children/{id}/force-background")
    @Operation(summary = "Принудительно установить фон ребёнку", description = "Без проверки уровня")
    public ResponseEntity<ChildResponseDto> forceSetBackground(
            @PathVariable UUID id,
            @RequestParam UUID backgroundOptionId
    ) {
        UUID currentAdminId = SecurityUtils.getCurrentUserId();
        log.info("Админ: принудительная смена фона ребёнка {}", id);
        ChildResponseDto child = childService.adminForceSetBackground(id, backgroundOptionId);
        UserResponseDto admin = userService.getById(currentAdminId);
        auditService.log(currentAdminId, admin.email(), AuditAction.CHILD_BACKGROUND_FORCED,
                "CHILD", id, "backgroundOptionId=" + backgroundOptionId);
        return ResponseEntity.ok(child);
    }
}
