package ru.taskhero.app.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.taskhero.app.dto.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign клиент для административных эндпоинтов User Service.
 */
@FeignClient(
        name = "user-service-admin",
        url = "${services.user-service.url}"
)
public interface AdminServiceClient {

    // ==================== Users ====================

    /** Получить всех пользователей. */
    @GetMapping("/admin/users")
    Map<String, Object> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active
    );

    /** Поиск пользователей. */
    @GetMapping("/admin/users/search")
    Map<String, Object> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    /** Заблокировать/разблокировать пользователя. */
    @PostMapping("/admin/users/{id}/toggle-active")
    AdminUserDto toggleUserActive(@PathVariable("id") UUID id);

    /** Изменить роль пользователя. */
    @PostMapping("/admin/users/{id}/role")
    AdminUserDto updateUserRole(@PathVariable("id") UUID id, @RequestBody Map<String, String> request);

    /** Удалить пользователя. */
    @DeleteMapping("/admin/users/{id}")
    void deleteUser(@PathVariable("id") UUID id);

    /** Сбросить пароль пользователя. */
    @PostMapping("/admin/users/{id}/password")
    void resetUserPassword(@PathVariable("id") UUID id, @RequestBody Map<String, String> request);

    // ==================== Parents ====================

    /** Получить всех родителей. */
    @GetMapping("/admin/parents")
    Map<String, Object> getAllParents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    );

    /** Получить детали родителя. */
    @GetMapping("/admin/parents/{id}")
    AdminParentDetailDto getParentDetail(@PathVariable("id") UUID id);

    /** Обновить данные родителя. */
    @PutMapping("/admin/parents/{id}")
    Map<String, Object> updateParent(@PathVariable("id") UUID id, @RequestBody Map<String, String> request);

    // ==================== Children ====================

    /** Получить всех детей. */
    @GetMapping("/admin/children")
    Map<String, Object> getAllChildren(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    );

    /** Получить детали ребёнка. */
    @GetMapping("/admin/children/{id}")
    Map<String, Object> getChildDetail(@PathVariable("id") UUID id);

    /** Обновить данные ребёнка. */
    @PutMapping("/admin/children/{id}")
    Map<String, Object> updateChild(@PathVariable("id") UUID id, @RequestBody Map<String, Object> request);

    /** Удалить ребёнка. */
    @DeleteMapping("/admin/children/{id}")
    void deleteChild(@PathVariable("id") UUID id);

    // ==================== Statistics ====================

    /** Получить статистику системы. */
    @GetMapping("/admin/statistics")
    AdminStatisticsDto getStatistics();

    // ==================== Audit ====================

    /** Создать запись аудита. */
    @PostMapping("/admin/audit")
    void createAuditEntry(@RequestBody Map<String, String> request);

    /** Получить журнал аудита с фильтрами. */
    @GetMapping("/admin/audit")
    Map<String, Object> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    );

    // ==================== Marketplace ====================

    /** Получить все товары маркетплейса. */
    @GetMapping("/admin/marketplace")
    List<Map<String, Object>> getMarketplaceItems();

    /** Создать товар в маркетплейсе. */
    @PostMapping("/admin/marketplace")
    Map<String, Object> createMarketplaceItem(@RequestBody Map<String, Object> request);

    /** Обновить товар маркетплейса. */
    @PutMapping("/admin/marketplace/{id}")
    Map<String, Object> updateMarketplaceItem(@PathVariable("id") UUID id, @RequestBody Map<String, Object> request);

    /** Удалить товар маркетплейса. */
    @DeleteMapping("/admin/marketplace/{id}")
    void deleteMarketplaceItem(@PathVariable("id") UUID id);

    /** Получить товары магазина родителя. */
    @GetMapping("/admin/parents/{id}/shop-items")
    List<Map<String, Object>> getParentShopItems(@PathVariable("id") UUID id);

    /** Получить покупки у родителя. */
    @GetMapping("/admin/parents/{id}/purchases")
    List<Map<String, Object>> getParentPurchases(@PathVariable("id") UUID id);

    // ==================== Avatar gallery ====================

    /** Получить все опции аватара, вкл. неактивные. */
    @GetMapping("/admin/avatar-options")
    List<AvatarOptionDto> getAvatarOptions();

    /** Обновить название аватара. */
    @PatchMapping("/admin/avatar-options/{id}/label")
    AvatarOptionDto updateAvatarOptionLabel(@PathVariable("id") UUID id, @RequestParam("label") String label);

    /** Включить/выключить видимость опции аватара. */
    @PatchMapping("/admin/avatar-options/{id}")
    AvatarOptionDto toggleAvatarOption(@PathVariable("id") UUID id, @RequestParam("active") boolean active);

    /** Удалить опцию аватара. */
    @DeleteMapping("/admin/avatar-options/{id}")
    void deleteAvatarOption(@PathVariable("id") UUID id);

    // ==================== Background gallery ====================

    /** Получить все фоны, вкл. неактивные. */
    @GetMapping("/admin/background-options")
    List<BackgroundOptionDto> getBackgroundOptions();

    /** Обновить название фона. */
    @PatchMapping("/admin/background-options/{id}/label")
    BackgroundOptionDto updateBackgroundOptionLabel(@PathVariable("id") UUID id, @RequestParam("label") String label);

    /** Включить/выключить видимость фона. */
    @PatchMapping("/admin/background-options/{id}")
    BackgroundOptionDto toggleBackgroundOption(@PathVariable("id") UUID id, @RequestParam("active") boolean active);

    /** Удалить фон. */
    @DeleteMapping("/admin/background-options/{id}")
    void deleteBackgroundOption(@PathVariable("id") UUID id);

    // ==================== Force-change avatar / background ====================

    /** Принудительно установить аватар ребёнку. */
    @PostMapping("/admin/children/{id}/force-avatar")
    Map<String, Object> forceSetAvatar(@PathVariable("id") UUID id, @RequestParam("avatarOptionId") UUID avatarOptionId);

    /** Принудительно установить фон ребёнку. */
    @PostMapping("/admin/children/{id}/force-background")
    Map<String, Object> forceSetBackground(@PathVariable("id") UUID id, @RequestParam("backgroundOptionId") UUID backgroundOptionId);
}
