package ru.taskhero.app.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.taskhero.app.client.AdminServiceClient;

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

    // ==================== Dashboard ====================

    /** Dashboard админ-панели. */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            var stats = adminServiceClient.getStatistics();
            model.addAttribute("stats", stats);

            var auditLogs = adminServiceClient.getAuditLogs(0, 10);
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
            adminServiceClient.updateUserRole(id, Map.of("role", role));
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
            Model model
    ) {
        try {
            var result = adminServiceClient.getAllParents(page, size);
            model.addAttribute("parents", result.get("content"));
            model.addAttribute("currentPage", result.get("number"));
            model.addAttribute("totalPages", result.get("totalPages"));
            model.addAttribute("totalElements", result.get("totalElements"));
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
            Model model
    ) {
        try {
            var result = adminServiceClient.getAllChildren(page, size);
            model.addAttribute("children", result.get("content"));
            model.addAttribute("currentPage", result.get("number"));
            model.addAttribute("totalPages", result.get("totalPages"));
            model.addAttribute("totalElements", result.get("totalElements"));
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
            Model model
    ) {
        try {
            var result = adminServiceClient.getAuditLogs(page, size);
            model.addAttribute("auditLogs", result.get("content"));
            model.addAttribute("currentPage", result.get("number"));
            model.addAttribute("totalPages", result.get("totalPages"));
            model.addAttribute("totalElements", result.get("totalElements"));
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
}
