package ru.taskhero.app.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.taskhero.app.client.AdminServiceClient;
import ru.taskhero.app.client.AvatarUploadClient;
import ru.taskhero.app.client.BackgroundUploadClient;
import ru.taskhero.app.client.TaskServiceClient;

import java.util.Map;
import java.util.UUID;

/**
 * Контроллер для административных страниц.
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private final AdminServiceClient adminServiceClient;
    private final TaskServiceClient taskServiceClient;
    private final AvatarUploadClient avatarUploadClient;
    private final BackgroundUploadClient backgroundUploadClient;

    // ==================== Dashboard ====================

    /** Dashboard админ-панели. */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            var stats = adminServiceClient.getStatistics();
            model.addAttribute("stats", stats);

            var auditLogs = adminServiceClient.getAuditLogs(0, 10, null, null, null);
            model.addAttribute("recentAudit", auditLogs.get("content"));
        } catch (Exception e) {
            log.error("Error loading admin dashboard: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки данных");
        }
        return "admin/dashboard";
    }

    // ==================== Users ====================

    /** Список пользователей. */
    @GetMapping("/users")
    public String users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q,
            Model model
    ) {
        try {
            Map<String, Object> result;
            if (q != null && !q.isBlank()) {
                result = adminServiceClient.searchUsers(q, page, size);
                model.addAttribute("searchQuery", q);
            } else {
                result = adminServiceClient.getAllUsers(page, size, role, active);
            }
            model.addAttribute("users", result.get("content"));
            model.addAttribute("currentPage", result.get("number"));
            model.addAttribute("totalPages", result.get("totalPages"));
            model.addAttribute("totalElements", result.get("totalElements"));
            model.addAttribute("selectedRole", role);
            model.addAttribute("selectedActive", active);
        } catch (Exception e) {
            log.error("Error loading users: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки пользователей");
        }
        return "admin/users";
    }

    /** Блокировка/разблокировка пользователя. */
    @PostMapping("/users/{id}/toggle-active")
    public String toggleUserActive(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            var user = adminServiceClient.toggleUserActive(id);
            redirectAttributes.addFlashAttribute("success",
                    "Пользователь " + user.email() + " " + (user.active() ? "разблокирован" : "заблокирован"));
        } catch (Exception e) {
            log.error("Error toggling user active: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/users";
    }

    /** Изменение роли пользователя. */
    @PostMapping("/users/{id}/role")
    public String updateUserRole(
            @PathVariable UUID id,
            @RequestParam String role,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.updateUserRole(id, Map.of("newRole", role));
            redirectAttributes.addFlashAttribute("success", "Роль обновлена");
        } catch (Exception e) {
            log.error("Error updating user role: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/users";
    }

    /** Удаление пользователя. */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            adminServiceClient.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Пользователь удалён");
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/users";
    }

    // ==================== Parents ====================

    /** Список родителей. */
    @GetMapping("/parents")
    public String parents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            Model model
    ) {
        try {
            var result = adminServiceClient.getAllParents(page, size, q);
            model.addAttribute("parents", result.get("content"));
            model.addAttribute("currentPage", result.get("number"));
            model.addAttribute("totalPages", result.get("totalPages"));
            model.addAttribute("totalElements", result.get("totalElements"));
            model.addAttribute("searchQuery", q);
        } catch (Exception e) {
            log.error("Error loading parents: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки родителей");
        }
        return "admin/parents";
    }

    /** Детали родителя. */
    @GetMapping("/parents/{id}")
    public String parentDetail(@PathVariable UUID id, Model model) {
        try {
            var parent = adminServiceClient.getParentDetail(id);
            model.addAttribute("parent", parent);

            try {
                var shopItems = adminServiceClient.getParentShopItems(id);
                model.addAttribute("shopItems", shopItems);
            } catch (Exception e) {
                log.warn("Не удалось загрузить товары родителя: {}", e.getMessage());
            }

            try {
                var templates = taskServiceClient.getParentTemplates(id);
                model.addAttribute("templates", templates);
            } catch (Exception e) {
                log.warn("Не удалось загрузить шаблоны родителя: {}", e.getMessage());
            }

            try {
                var assignments = taskServiceClient.getParentAssignments(id);
                model.addAttribute("assignments", assignments);
            } catch (Exception e) {
                log.warn("Не удалось загрузить назначения родителя: {}", e.getMessage());
            }

            try {
                var purchases = adminServiceClient.getParentPurchases(id);
                model.addAttribute("purchases", purchases);
            } catch (Exception e) {
                log.warn("Не удалось загрузить покупки родителя: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error loading parent detail: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки данных родителя");
        }
        return "admin/parent-detail";
    }

    /** Обновление данных родителя. */
    @PostMapping("/parents/{id}/edit")
    public String updateParent(
            @PathVariable UUID id,
            @RequestParam String firstName,
            @RequestParam String surname,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.updateParent(id, Map.of("firstName", firstName, "surname", surname));
            redirectAttributes.addFlashAttribute("success", "Данные родителя обновлены");
        } catch (Exception e) {
            log.error("Error updating parent: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/parents/" + id;
    }

    // ==================== Children ====================

    /** Список детей. */
    @GetMapping("/children")
    public String children(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            Model model
    ) {
        try {
            var result = adminServiceClient.getAllChildren(page, size, q);
            model.addAttribute("children", result.get("content"));
            model.addAttribute("currentPage", result.get("number"));
            model.addAttribute("totalPages", result.get("totalPages"));
            model.addAttribute("totalElements", result.get("totalElements"));
            model.addAttribute("searchQuery", q);
        } catch (Exception e) {
            log.error("Error loading children: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки детей");
        }
        return "admin/children";
    }

    /** Детали ребёнка. */
    @GetMapping("/children/{id}")
    public String childDetail(@PathVariable UUID id, Model model) {
        try {
            var child = adminServiceClient.getChildDetail(id);
            model.addAttribute("child", child);

            try {
                var assignments = taskServiceClient.getChildAssignments(id);
                model.addAttribute("assignments", assignments);
            } catch (Exception e) {
                log.warn("Не удалось загрузить назначения ребёнка: {}", e.getMessage());
            }

            try {
                model.addAttribute("allAvatarOptions", adminServiceClient.getAvatarOptions());
            } catch (Exception e) {
                log.warn("Не удалось загрузить галерею аватаров: {}", e.getMessage());
            }

            try {
                model.addAttribute("allBackgroundOptions", adminServiceClient.getBackgroundOptions());
            } catch (Exception e) {
                log.warn("Не удалось загрузить галерею фонов: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error loading child detail: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки данных ребёнка");
        }
        return "admin/child-detail";
    }

    /** Удаление ребёнка. */
    @PostMapping("/children/{id}/delete")
    public String deleteChild(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            adminServiceClient.deleteChild(id);
            redirectAttributes.addFlashAttribute("success", "Ребёнок удалён");
        } catch (Exception e) {
            log.error("Error deleting child: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/children";
    }

    // ==================== Statistics ====================

    /** Статистика системы. */
    @GetMapping("/statistics")
    public String statistics(Model model) {
        try {
            var stats = adminServiceClient.getStatistics();
            model.addAttribute("stats", stats);
        } catch (Exception e) {
            log.error("Error loading statistics: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки статистики");
        }
        return "admin/statistics";
    }

    // ==================== Audit Log ====================

    /** Журнал аудита. */
    @GetMapping("/audit")
    public String auditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Model model
    ) {
        try {
            String isoFrom = dateFrom != null && !dateFrom.isBlank() ? dateFrom + "T00:00:00Z" : null;
            String isoTo = dateTo != null && !dateTo.isBlank() ? dateTo + "T23:59:59Z" : null;
            String actionParam = action != null && !action.isBlank() ? action : null;

            var result = adminServiceClient.getAuditLogs(page, size, actionParam, isoFrom, isoTo);
            model.addAttribute("auditLogs", result.get("content"));
            model.addAttribute("currentPage", result.get("number"));
            model.addAttribute("totalPages", result.get("totalPages"));
            model.addAttribute("totalElements", result.get("totalElements"));
            model.addAttribute("selectedAction", action);
            model.addAttribute("dateFrom", dateFrom);
            model.addAttribute("dateTo", dateTo);
        } catch (Exception e) {
            log.error("Error loading audit log: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки журнала аудита");
        }
        return "admin/audit";
    }

    /** Извлечь сообщение об ошибке из Feign-исключения. */
    private String extractMessage(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("\"message\":\"")) {
            int start = msg.indexOf("\"message\":\"") + 11;
            int end = msg.indexOf("\"", start);
            if (end > start) {
                return msg.substring(start, end);
            }
        }
        return msg != null ? msg : "Произошла ошибка";
    }

    // ==================== Password Reset ====================

    /** Сброс пароля пользователя. */
    @PostMapping("/users/{id}/password")
    public String resetPassword(
            @PathVariable UUID id,
            @RequestParam String password,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (password == null || password.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Пароль должен содержать минимум 6 символов");
                return "redirect:/admin/users";
            }
            adminServiceClient.resetUserPassword(id, Map.of("password", password));
            redirectAttributes.addFlashAttribute("success", "Пароль пользователя успешно изменён");
        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/users";
    }

    // ==================== Marketplace ====================

    /** Страница маркетплейса наград. */
    @GetMapping("/marketplace")
    public String marketplace(Model model) {
        try {
            var items = adminServiceClient.getMarketplaceItems();
            model.addAttribute("items", items);
        } catch (Exception e) {
            log.error("Error loading marketplace: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки маркетплейса");
        }
        return "admin/marketplace";
    }

    /** Создание товара маркетплейса. */
    @PostMapping("/marketplace")
    public String createMarketplaceItem(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam int priceCoins,
            @RequestParam(required = false) String iconName,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.createMarketplaceItem(Map.of(
                    "title", title,
                    "description", description != null ? description : "",
                    "priceCoins", priceCoins,
                    "iconName", normalizeBootstrapIcon(iconName)
            ));
            redirectAttributes.addFlashAttribute("success", "Товар создан");
        } catch (Exception e) {
            log.error("Error creating marketplace item: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/marketplace";
    }

    /** Обновление товара маркетплейса. */
    @PostMapping("/marketplace/{id}/edit")
    public String updateMarketplaceItem(
            @PathVariable UUID id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam int priceCoins,
            @RequestParam(required = false) String iconName,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.updateMarketplaceItem(id, Map.of(
                    "title", title,
                    "description", description != null ? description : "",
                    "priceCoins", priceCoins,
                    "iconName", normalizeBootstrapIcon(iconName)
            ));
            redirectAttributes.addFlashAttribute("success", "Товар обновлён");
        } catch (Exception e) {
            log.error("Error updating marketplace item: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/marketplace";
    }

    /** Удаление товара маркетплейса. */
    @PostMapping("/marketplace/{id}/delete")
    public String deleteMarketplaceItem(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            adminServiceClient.deleteMarketplaceItem(id);
            redirectAttributes.addFlashAttribute("success", "Товар удалён");
        } catch (Exception e) {
            log.error("Error deleting marketplace item: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/marketplace";
    }

    // ==================== Template Library ====================

    /** Страница библиотеки шаблонов. */
    @GetMapping("/templates")
    public String templates(Model model) {
        try {
            var templates = taskServiceClient.getAdminLibraryTemplates();
            model.addAttribute("templates", templates);
        } catch (Exception e) {
            log.error("Error loading templates: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки шаблонов");
        }
        return "admin/templates";
    }

    /** Создание шаблона в библиотеке. */
    @PostMapping("/templates")
    public String createTemplate(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer expReward,
            @RequestParam(required = false) Integer coinsReward,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String presetGroup,
            RedirectAttributes redirectAttributes
    ) {
        try {
            java.util.HashMap<String, Object> req = new java.util.HashMap<>();
            req.put("title", title);
            if (description != null) req.put("description", description);
            if (expReward != null) req.put("expReward", expReward);
            if (coinsReward != null) req.put("coinsReward", coinsReward);
            if (category != null && !category.isBlank()) req.put("category", category);
            if (difficulty != null && !difficulty.isBlank()) req.put("difficulty", difficulty);
            if (presetGroup != null && !presetGroup.isBlank()) req.put("presetGroup", presetGroup);
            taskServiceClient.createLibraryTemplate(req);
            redirectAttributes.addFlashAttribute("success", "Шаблон создан");
        } catch (Exception e) {
            log.error("Error creating template: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/templates";
    }

    /** Обновление шаблона библиотеки. */
    @PostMapping("/templates/{id}/edit")
    public String updateTemplate(
            @PathVariable UUID id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer expReward,
            @RequestParam(required = false) Integer coinsReward,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String presetGroup,
            RedirectAttributes redirectAttributes
    ) {
        try {
            java.util.HashMap<String, Object> req = new java.util.HashMap<>();
            req.put("title", title);
            if (description != null) req.put("description", description);
            if (expReward != null) req.put("expReward", expReward);
            if (coinsReward != null) req.put("coinsReward", coinsReward);
            if (category != null && !category.isBlank()) req.put("category", category);
            if (difficulty != null && !difficulty.isBlank()) req.put("difficulty", difficulty);
            if (presetGroup != null && !presetGroup.isBlank()) req.put("presetGroup", presetGroup);
            taskServiceClient.updateLibraryTemplate(id, req);
            redirectAttributes.addFlashAttribute("success", "Шаблон обновлён");
        } catch (Exception e) {
            log.error("Error updating template: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/templates";
    }

    /** Удаление шаблона библиотеки. */
    @PostMapping("/templates/{id}/delete")
    public String deleteTemplate(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            taskServiceClient.deleteLibraryTemplate(id);
            redirectAttributes.addFlashAttribute("success", "Шаблон удалён");
        } catch (Exception e) {
            log.error("Error deleting template: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/templates";
    }

    private String normalizeBootstrapIcon(String iconName) {
        return iconName != null && !iconName.isBlank() ? iconName.trim() : "bi-gift";
    }

    // ==================== Avatar Gallery ====================

    /** Страница галереи аватаров. */
    @GetMapping("/avatar-options")
    public String avatarOptions(Model model) {
        try {
            model.addAttribute("avatarOptions", adminServiceClient.getAvatarOptions());
        } catch (Exception e) {
            log.error("Error loading avatar options: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки галереи аватаров");
        }
        return "admin/avatar-options";
    }

    /** Загрузка новой картинки в галерею аватаров. */
    @PostMapping("/avatar-options")
    public String uploadAvatarOption(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(required = false) String label,
            RedirectAttributes redirectAttributes
    ) {
        try {
            avatarUploadClient.upload(file, label);
            redirectAttributes.addFlashAttribute("success", "Аватар загружен");
        } catch (Exception e) {
            log.error("Error uploading avatar option: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/avatar-options";
    }

    /** Переименовать аватар. */
    @PostMapping("/avatar-options/{id}/rename")
    public String renameAvatarOption(
            @PathVariable UUID id,
            @RequestParam String label,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.updateAvatarOptionLabel(id, label);
            redirectAttributes.addFlashAttribute("success", "Название аватара обновлено");
        } catch (Exception e) {
            log.error("Error renaming avatar option: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/avatar-options";
    }

    /** Включить/выключить видимость аватара в пикере ребёнка. */
    @PostMapping("/avatar-options/{id}/toggle")
    public String toggleAvatarOption(
            @PathVariable UUID id,
            @RequestParam boolean active,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.toggleAvatarOption(id, active);
            redirectAttributes.addFlashAttribute("success", active ? "Аватар активирован" : "Аватар скрыт");
        } catch (Exception e) {
            log.error("Error toggling avatar option: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/avatar-options";
    }

    /** Удаление аватара из галереи. */
    @PostMapping("/avatar-options/{id}/delete")
    public String deleteAvatarOption(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            adminServiceClient.deleteAvatarOption(id);
            redirectAttributes.addFlashAttribute("success", "Аватар удалён");
        } catch (Exception e) {
            log.error("Error deleting avatar option: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/avatar-options";
    }

    // ==================== Background Gallery ====================

    /** Страница галереи фонов. */
    @GetMapping("/background-options")
    public String backgroundOptions(Model model) {
        try {
            model.addAttribute("backgroundOptions", adminServiceClient.getBackgroundOptions());
        } catch (Exception e) {
            log.error("Error loading background options: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки галереи фонов");
        }
        return "admin/background-options";
    }

    /** Загрузка нового фона. */
    @PostMapping("/background-options")
    public String uploadBackgroundOption(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(required = false) String label,
            RedirectAttributes redirectAttributes
    ) {
        try {
            backgroundUploadClient.upload(file, label);
            redirectAttributes.addFlashAttribute("success", "Фон загружен");
        } catch (Exception e) {
            log.error("Error uploading background option: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/background-options";
    }

    /** Переименовать фон. */
    @PostMapping("/background-options/{id}/rename")
    public String renameBackgroundOption(
            @PathVariable UUID id,
            @RequestParam String label,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.updateBackgroundOptionLabel(id, label);
            redirectAttributes.addFlashAttribute("success", "Название фона обновлено");
        } catch (Exception e) {
            log.error("Error renaming background option: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/background-options";
    }

    /** Включить/выключить фон. */
    @PostMapping("/background-options/{id}/toggle")
    public String toggleBackgroundOption(
            @PathVariable UUID id,
            @RequestParam boolean active,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.toggleBackgroundOption(id, active);
            redirectAttributes.addFlashAttribute("success", active ? "Фон активирован" : "Фон скрыт");
        } catch (Exception e) {
            log.error("Error toggling background option: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/background-options";
    }

    /** Удаление фона из галереи. */
    @PostMapping("/background-options/{id}/delete")
    public String deleteBackgroundOption(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            adminServiceClient.deleteBackgroundOption(id);
            redirectAttributes.addFlashAttribute("success", "Фон удалён");
        } catch (Exception e) {
            log.error("Error deleting background option: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/background-options";
    }

    // ==================== Admin force-change avatar/background ====================

    /** Принудительно поменять аватар ребёнку. */
    @PostMapping("/children/{id}/force-avatar")
    public String forceSetAvatar(
            @PathVariable UUID id,
            @RequestParam UUID avatarOptionId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.forceSetAvatar(id, avatarOptionId);
            redirectAttributes.addFlashAttribute("success", "Аватар изменён");
        } catch (Exception e) {
            log.error("Error force-setting avatar: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/children/" + id;
    }

    /** Принудительно поменять фон ребёнку. */
    @PostMapping("/children/{id}/force-background")
    public String forceSetBackground(
            @PathVariable UUID id,
            @RequestParam UUID backgroundOptionId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminServiceClient.forceSetBackground(id, backgroundOptionId);
            redirectAttributes.addFlashAttribute("success", "Фон изменён");
        } catch (Exception e) {
            log.error("Error force-setting background: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e));
        }
        return "redirect:/admin/children/" + id;
    }
}
