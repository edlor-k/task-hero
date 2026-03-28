package ru.taskhero.app.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.taskhero.app.client.AdminServiceClient;
import ru.taskhero.app.client.TaskServiceClient;
import ru.taskhero.app.client.UserServiceClient;
import ru.taskhero.app.dto.ChildDto;
import ru.taskhero.app.dto.CreateChildRequest;
import ru.taskhero.app.dto.LevelInfoDto;
import ru.taskhero.app.dto.LevelRewardDto;
import ru.taskhero.app.dto.ShopItemDto;
import ru.taskhero.app.dto.ShopPurchaseDto;
import ru.taskhero.app.dto.TaskAssignmentDto;
import ru.taskhero.app.dto.TaskTemplateDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Контроллер для страниц родителя.
 */
@Slf4j
@Controller
@RequestMapping("/parent")
@RequiredArgsConstructor
public class ParentController {

    private final UserServiceClient userServiceClient;
    private final TaskServiceClient taskServiceClient;
    private final AdminServiceClient adminServiceClient;

    /**
     * Добавляет атрибут onboardingComplete для всех страниц родителя.
     * Используется для скрытия пункта меню «Онбординг» после завершения настройки.
     */
    @ModelAttribute("onboardingComplete")
    public boolean isOnboardingComplete() {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            if (children.isEmpty()) return false;
            boolean hasLevelRewards = children.stream().allMatch(child -> {
                try {
                    List<LevelRewardDto> rewards = userServiceClient.getLevelRewards(child.id());
                    return rewards != null && rewards.size() >= 9;
                } catch (Exception e) {
                    return false;
                }
            });
            boolean hasShopItems = !userServiceClient.getShopItems().isEmpty();
            boolean hasTemplates = !taskServiceClient.getActiveTemplates().isEmpty();
            return hasLevelRewards && hasShopItems && hasTemplates;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Страница онбординга для новых родителей.
     */
    @GetMapping("/onboarding")
    public String onboarding(Model model) {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            List<ShopItemDto> shopItems = userServiceClient.getShopItems();
            List<TaskTemplateDto> templates = taskServiceClient.getActiveTemplates();

            boolean hasChildren = !children.isEmpty();
            boolean hasLevelRewards = false;
            if (hasChildren) {
                hasLevelRewards = children.stream().allMatch(child -> {
                    try {
                        List<LevelRewardDto> rewards = userServiceClient.getLevelRewards(child.id());
                        return rewards != null && rewards.size() >= 9;
                    } catch (Exception e) {
                        return false;
                    }
                });
            }

            model.addAttribute("hasChildren", hasChildren);
            model.addAttribute("hasLevelRewards", hasLevelRewards);
            model.addAttribute("hasShopItems", !shopItems.isEmpty());
            model.addAttribute("hasTemplates", !templates.isEmpty());
            model.addAttribute("children", children);
        } catch (Exception e) {
            log.error("Error loading onboarding: {}", e.getMessage());
            model.addAttribute("hasChildren", false);
            model.addAttribute("hasLevelRewards", false);
            model.addAttribute("hasShopItems", false);
            model.addAttribute("hasTemplates", false);
        }
        return "parent/onboarding";
    }

    /**
     * Dashboard родителя.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            List<TaskAssignmentDto> pendingReview = taskServiceClient.getPendingReview();

            model.addAttribute("children", children);
            model.addAttribute("pendingReview", pendingReview);
            model.addAttribute("pendingCount", pendingReview.size());

            // Check onboarding completeness
            boolean hasChildren = !children.isEmpty();
            boolean hasLevelRewards = false;
            boolean hasShopItems = false;
            boolean hasTemplates = false;
            try {
                hasShopItems = !userServiceClient.getShopItems().isEmpty();
                hasTemplates = !taskServiceClient.getActiveTemplates().isEmpty();
                if (hasChildren) {
                    hasLevelRewards = children.stream().allMatch(child -> {
                        try {
                            List<LevelRewardDto> rewards = userServiceClient.getLevelRewards(child.id());
                            return rewards != null && rewards.size() >= 9;
                        } catch (Exception ex) {
                            return false;
                        }
                    });
                }
            } catch (Exception ex) {
                log.warn("Error checking onboarding status: {}", ex.getMessage());
            }
            boolean onboardingIncomplete = !hasChildren || !hasLevelRewards || !hasShopItems || !hasTemplates;
            model.addAttribute("onboardingIncomplete", onboardingIncomplete);

            // Check unfilled level rewards for each child
            List<Map<String, Object>> childRewardAlerts = new ArrayList<>();
            for (ChildDto child : children) {
                try {
                    Map<String, Integer> unfilledInfo = userServiceClient.getUnfilledCount(child.id());
                    int unfilledCount = unfilledInfo.getOrDefault("unfilledCount", 0);
                    if (unfilledCount < 5) {
                        Map<String, Object> alert = new HashMap<>();
                        alert.put("childName", child.firstName());
                        alert.put("childId", child.id());
                        alert.put("unfilledCount", unfilledCount);
                        childRewardAlerts.add(alert);
                    }
                } catch (Exception ex) {
                    log.warn("Error checking unfilled rewards for child {}: {}", child.id(), ex.getMessage());
                }
            }
            model.addAttribute("childRewardAlerts", childRewardAlerts);

            // Check for expired (overdue) assignments
            try {
                List<TaskAssignmentDto> expiredAssignments = taskServiceClient.getExpiredAssignments();
                model.addAttribute("expiredAssignments", expiredAssignments);
            } catch (Exception ex) {
                log.warn("Error loading expired assignments: {}", ex.getMessage());
            }

        } catch (Exception e) {
            log.error("Error loading dashboard: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки данных");
        }

        return "parent/dashboard";
    }

    // ==================== Children ====================

    /**
     * Список детей.
     */
    @GetMapping("/children")
    public String children(@RequestParam(required = false) boolean onboarding, Model model) {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            model.addAttribute("children", children);
            model.addAttribute("onboarding", onboarding);
        } catch (Exception e) {
            log.error("Error loading children: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки списка детей");
        }
        return "parent/children";
    }

    /**
     * Добавить ребёнка.
     */
    @PostMapping("/children/add")
    public String addChild(
            @RequestParam String firstName,
            @RequestParam String surname,
            @RequestParam(defaultValue = "false") boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        try {
            CreateChildRequest request = new CreateChildRequest(firstName, surname, "NORMAL");
            ChildDto child = userServiceClient.createChild(request);

            redirectAttributes.addFlashAttribute("success",
                    "Ребёнок добавлен! Токен для входа: " + child.loginToken());

        } catch (Exception e) {
            log.error("Error adding child: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Ошибка добавления ребёнка";
            redirectAttributes.addFlashAttribute("error", msg);
        }

        if (onboarding) {
            return "redirect:/parent/onboarding";
        }
        return "redirect:/parent/children";
    }

    /**
     * Профиль ребёнка.
     */
    @GetMapping("/children/{id}")
    public String childProfile(@PathVariable UUID id, Model model) {
        try {
            ChildDto child = userServiceClient.getChildById(id);
            List<TaskAssignmentDto> assignments = taskServiceClient.getChildAssignments(id, null);

            model.addAttribute("child", child);
            model.addAttribute("assignments", assignments);

        } catch (Exception e) {
            log.error("Error loading child profile: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки профиля ребёнка");
        }

        return "parent/child-profile";
    }

    // ==================== Templates ====================

    /**
     * Список шаблонов заданий.
     */
    @GetMapping("/templates")
    public String templates(@RequestParam(required = false) boolean onboarding, Model model) {
        try {
            Map<String, Object> response = taskServiceClient.getTemplates(0, 50);
            model.addAttribute("templates", response.get("content"));
            model.addAttribute("onboarding", onboarding);
        } catch (Exception e) {
            log.error("Error loading templates: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки шаблонов");
        }
        return "parent/templates";
    }

    /**
     * Создать шаблон.
     */
    @PostMapping("/templates/add")
    public String addTemplate(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "10") int expReward,
            @RequestParam(defaultValue = "5") int coinsReward,
            @RequestParam(defaultValue = "false") boolean repeatable,
            @RequestParam(required = false) Integer repeatCount,
            @RequestParam(required = false) Integer repeatInterval,
            @RequestParam(required = false) String repeatUnit,
            @RequestParam(defaultValue = "NORMAL") String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String recurrenceRule,
            @RequestParam(required = false) List<String> subItems,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = buildTemplateRequest(title, description, expReward, coinsReward,
                    repeatable, repeatCount, repeatInterval, repeatUnit,
                    difficulty, category, recurrenceRule, subItems);

            taskServiceClient.createTemplate(request);

            redirectAttributes.addFlashAttribute("success", "Шаблон создан!");

        } catch (Exception e) {
            log.error("Error creating template: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Ошибка создания шаблона";
            redirectAttributes.addFlashAttribute("error", msg);
        }

        return "redirect:/parent/templates";
    }

    /**
     * Обновить шаблон.
     */
    @PostMapping("/templates/{id}/edit")
    public String editTemplate(
            @PathVariable UUID id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "10") int expReward,
            @RequestParam(defaultValue = "5") int coinsReward,
            @RequestParam(defaultValue = "NORMAL") String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> subItems,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("title", title);
            request.put("description", description);
            request.put("expReward", expReward);
            request.put("coinsReward", coinsReward);
            request.put("difficulty", difficulty);
            if (category != null && !category.isBlank()) {
                request.put("category", category);
            }
            if (subItems != null && !subItems.isEmpty()) {
                List<String> filtered = subItems.stream()
                        .filter(s -> s != null && !s.isBlank())
                        .toList();
                if (!filtered.isEmpty()) {
                    request.put("subItems", filtered);
                }
            }

            taskServiceClient.updateTemplate(id, request);
            redirectAttributes.addFlashAttribute("success", "Шаблон обновлён!");

        } catch (Exception e) {
            log.error("Error updating template: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Ошибка обновления шаблона";
            redirectAttributes.addFlashAttribute("error", msg);
        }

        return "redirect:/parent/templates";
    }

    /**
     * Удалить шаблон.
     */
    @PostMapping("/templates/{id}/delete")
    public String deleteTemplate(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            taskServiceClient.deleteTemplate(id);
            redirectAttributes.addFlashAttribute("success", "Шаблон удалён!");
        } catch (Exception e) {
            log.error("Error deleting template: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Ошибка удаления шаблона";
            redirectAttributes.addFlashAttribute("error", msg);
        }
        return "redirect:/parent/templates";
    }

    private Map<String, Object> buildTemplateRequest(
            String title, String description, int expReward, int coinsReward,
            boolean repeatable, Integer repeatCount, Integer repeatInterval, String repeatUnit,
            String difficulty, String category, String recurrenceRule, List<String> subItems
    ) {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        request.put("description", description);
        request.put("expReward", expReward);
        request.put("coinsReward", coinsReward);
        request.put("repeatable", repeatable);
        request.put("difficulty", difficulty);
        if (category != null && !category.isBlank()) {
            request.put("category", category);
        }
        if (recurrenceRule != null && !recurrenceRule.isBlank()) {
            request.put("recurrenceRule", recurrenceRule);
            request.put("repeatable", true);
        }
        if (subItems != null && !subItems.isEmpty()) {
            List<String> filtered = subItems.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .toList();
            if (!filtered.isEmpty()) {
                request.put("subItems", filtered);
            }
        }
        if (repeatable && repeatCount != null) {
            request.put("repeatCount", repeatCount);
            request.put("repeatInterval", repeatInterval);
            request.put("repeatUnit", repeatUnit);
        }
        return request;
    }

    // ==================== Library ====================

    /**
     * Страница библиотеки заданий.
     */
    @GetMapping("/library")
    public String libraryPage(@RequestParam(required = false) String category,
                              @RequestParam(required = false) boolean onboarding,
                              Model model) {
        try {
            List<TaskTemplateDto> libraryTemplates = taskServiceClient.getLibraryTemplates(category);
            Set<UUID> copiedIds = taskServiceClient.getCopiedLibraryTemplateIds();

            List<TaskTemplateDto> availableTemplates = libraryTemplates.stream()
                    .filter(t -> !copiedIds.contains(t.id()))
                    .toList();
            List<TaskTemplateDto> addedTemplates = libraryTemplates.stream()
                    .filter(t -> copiedIds.contains(t.id()))
                    .toList();

            model.addAttribute("libraryTemplates", availableTemplates);
            model.addAttribute("addedTemplates", addedTemplates);
            model.addAttribute("selectedCategory", category);
            model.addAttribute("onboarding", onboarding);
        } catch (Exception e) {
            log.error("Error loading library: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки библиотеки");
        }
        return "parent/library";
    }

    /**
     * Скопировать шаблон из библиотеки.
     */
    @PostMapping("/library/copy")
    public String copyFromLibrary(
            @RequestParam UUID templateId,
            @RequestParam(required = false) boolean onboarding,
            @RequestParam(required = false) String category,
            RedirectAttributes redirectAttributes
    ) {
        try {
            taskServiceClient.copyFromLibrary(templateId);
            redirectAttributes.addFlashAttribute("success", "Шаблон скопирован в ваши задания!");
        } catch (Exception e) {
            log.error("Error copying from library: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка копирования шаблона");
        }
        StringBuilder redirect = new StringBuilder("redirect:/parent/library");
        String sep = "?";
        if (onboarding) { redirect.append(sep).append("onboarding=true"); sep = "&"; }
        if (category != null && !category.isBlank()) { redirect.append(sep).append("category=").append(category); }
        return redirect.toString();
    }

    // ==================== Assignments ====================

    /**
     * Страница назначения задания.
     */
    @GetMapping("/assign")
    public String assignPage(Model model) {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            List<TaskTemplateDto> templates = taskServiceClient.getActiveTemplates();

            model.addAttribute("children", children);
            model.addAttribute("templates", templates);

        } catch (Exception e) {
            log.error("Error loading assign page: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки данных");
        }

        return "parent/assign";
    }

    /**
     * Назначить задание.
     */
    @PostMapping("/assign")
    public String assignTask(
            @RequestParam UUID childId,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) String recurrenceRule,
            @RequestParam(required = false) String quickTitle,
            @RequestParam(required = false) String quickDescription,
            @RequestParam(defaultValue = "10") int quickExpReward,
            @RequestParam(defaultValue = "5") int quickCoinsReward,
            @RequestParam(defaultValue = "NORMAL") String quickDifficulty,
            @RequestParam(required = false) String quickCategory,
            @RequestParam(defaultValue = "false") boolean saveAsTemplate,
            @RequestParam(defaultValue = "false") boolean important,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UUID finalTemplateId = templateId;

            // Быстрое создание разового задания
            if (finalTemplateId == null && quickTitle != null && !quickTitle.isBlank()) {
                Map<String, Object> templateRequest = new HashMap<>();
                templateRequest.put("title", quickTitle);
                templateRequest.put("description", quickDescription);
                templateRequest.put("expReward", quickExpReward);
                templateRequest.put("coinsReward", quickCoinsReward);
                templateRequest.put("difficulty", quickDifficulty);
                if (quickCategory != null && !quickCategory.isBlank()) {
                    templateRequest.put("category", quickCategory);
                }
                if (!saveAsTemplate) {
                    templateRequest.put("active", false); // Разовое, не сохранять как активный шаблон
                }
                TaskTemplateDto created = taskServiceClient.createTemplate(templateRequest);
                finalTemplateId = created.id();
            }

            if (finalTemplateId == null) {
                redirectAttributes.addFlashAttribute("error", "Выберите задание или создайте новое");
                return "redirect:/parent/assign";
            }

            Map<String, Object> request = new HashMap<>();
            request.put("templateId", finalTemplateId.toString());
            request.put("childId", childId.toString());
            if (dueDate != null && !dueDate.isEmpty()) {
                request.put("dueDate", dueDate);
            }
            request.put("important", important);

            taskServiceClient.assignTask(request);

            try {
                Map<String, String> audit = new HashMap<>();
                audit.put("action", "TASK_ASSIGNED");
                audit.put("targetType", "ASSIGNMENT");
                audit.put("details", "Назначено задание ребёнку " + childId);
                adminServiceClient.createAuditEntry(audit);
            } catch (Exception ex) {
                log.warn("Audit log failed: {}", ex.getMessage());
            }

            redirectAttributes.addFlashAttribute("success", "Задание назначено!");

        } catch (Exception e) {
            log.error("Error assigning task: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка назначения задания");
        }

        return "redirect:/parent/dashboard";
    }

    /**
     * Страница проверки заданий.
     */
    @GetMapping("/review")
    public String reviewPage(Model model) {
        try {
            List<TaskAssignmentDto> pendingReview = taskServiceClient.getPendingReview();
            model.addAttribute("assignments", pendingReview);
        } catch (Exception e) {
            log.error("Error loading review page: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки заданий");
        }
        return "parent/review";
    }

    /**
     * Одобрить задание.
     */
    @PostMapping("/assignments/{id}/approve")
    public String approveTask(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            if (comment != null && !comment.isEmpty()) {
                request.put("comment", comment);
            }

            taskServiceClient.approveTask(id, request);

            try {
                Map<String, String> audit = new HashMap<>();
                audit.put("action", "TASK_APPROVED");
                audit.put("targetType", "ASSIGNMENT");
                audit.put("targetId", id.toString());
                audit.put("details", "Одобрено задание " + id);
                adminServiceClient.createAuditEntry(audit);
            } catch (Exception ex) {
                log.warn("Audit log failed: {}", ex.getMessage());
            }

            redirectAttributes.addFlashAttribute("success", "Задание одобрено!");

        } catch (Exception e) {
            log.error("Error approving task: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка одобрения");
        }

        return "redirect:/parent/review";
    }

    /**
     * Отклонить задание.
     */
    @PostMapping("/assignments/{id}/reject")
    public String rejectTask(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            if (comment != null && !comment.isEmpty()) {
                request.put("comment", comment);
            }

            taskServiceClient.rejectTask(id, request);

            try {
                Map<String, String> audit = new HashMap<>();
                audit.put("action", "TASK_REJECTED");
                audit.put("targetType", "ASSIGNMENT");
                audit.put("targetId", id.toString());
                audit.put("details", "Отклонено задание " + id);
                adminServiceClient.createAuditEntry(audit);
            } catch (Exception ex) {
                log.warn("Audit log failed: {}", ex.getMessage());
            }

            redirectAttributes.addFlashAttribute("success", "Задание отклонено");

        } catch (Exception e) {
            log.error("Error rejecting task: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка отклонения");
        }

        return "redirect:/parent/review";
    }

    /**
     * Продлить дедлайн просроченного задания.
     */
    @PostMapping("/assignments/{id}/extend-deadline")
    public String extendDeadline(
            @PathVariable UUID id,
            @RequestParam String newDueDate,
            @RequestParam(required = false) Integer newExpReward,
            @RequestParam(required = false) Integer newCoinsReward,
            RedirectAttributes redirectAttributes
    ) {
        try {
            taskServiceClient.extendDeadline(id, newDueDate);

            // Update template rewards if provided
            if (newExpReward != null || newCoinsReward != null) {
                try {
                    TaskAssignmentDto assignment = taskServiceClient.getAssignment(id);
                    if (assignment.template() != null) {
                        Map<String, Object> updateRequest = new HashMap<>();
                        updateRequest.put("title", assignment.template().title());
                        updateRequest.put("description", assignment.template().description());
                        updateRequest.put("expReward", newExpReward != null ? newExpReward : assignment.template().expReward());
                        updateRequest.put("coinsReward", newCoinsReward != null ? newCoinsReward : assignment.template().coinsReward());
                        updateRequest.put("difficulty", assignment.template().difficulty());
                        taskServiceClient.updateTemplate(assignment.template().id(), updateRequest);
                    }
                } catch (Exception ex) {
                    log.warn("Error updating template rewards: {}", ex.getMessage());
                }
            }

            redirectAttributes.addFlashAttribute("success", "Дедлайн продлён");
        } catch (Exception e) {
            log.error("Error extending deadline: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка продления дедлайна");
        }
        return "redirect:/parent/dashboard";
    }

    /**
     * Удалить назначение задания.
     */
    @PostMapping("/assignments/{id}/delete")
    public String deleteAssignment(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            taskServiceClient.deleteAssignment(id);
            redirectAttributes.addFlashAttribute("success", "Задание удалено");
        } catch (Exception e) {
            log.error("Error deleting assignment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления задания");
        }
        return "redirect:/parent/dashboard";
    }

    // ==================== Shop ====================

    /**
     * Страница магазина родителя.
     */
    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) boolean onboarding, Model model) {
        try {
            List<ShopItemDto> items = userServiceClient.getShopItems();
            List<ShopPurchaseDto> pendingPurchases = userServiceClient.getPendingPurchases();
            List<ShopPurchaseDto> allPurchases = userServiceClient.getAllPurchases();
            List<ChildDto> children = userServiceClient.getChildren();

            model.addAttribute("items", items);
            model.addAttribute("pendingPurchases", pendingPurchases);
            model.addAttribute("allPurchases", allPurchases);
            model.addAttribute("children", children);
            model.addAttribute("onboarding", onboarding);
        } catch (Exception e) {
            log.error("Error loading shop: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки магазина");
        }
        return "parent/shop";
    }

    /**
     * Добавить товар в магазин.
     */
    @PostMapping("/shop/add")
    public String addShopItem(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam int priceCoins,
            @RequestParam(defaultValue = "bi-gift") String iconName,
            @RequestParam List<UUID> childIds,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("title", title);
            request.put("description", description);
            request.put("priceCoins", priceCoins);
            request.put("iconName", iconName);
            request.put("childIds", childIds.stream().map(UUID::toString).toList());

            userServiceClient.createShopItem(request);
            redirectAttributes.addFlashAttribute("success", "Товар добавлен!");
        } catch (Exception e) {
            log.error("Error adding shop item: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка добавления товара");
        }
        return "redirect:/parent/shop";
    }

    /**
     * Удалить товар из магазина.
     */
    @PostMapping("/shop/{id}/delete")
    public String deleteShopItem(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.deleteShopItem(id);
            redirectAttributes.addFlashAttribute("success", "Товар удалён!");
        } catch (Exception e) {
            log.error("Error deleting shop item: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления товара");
        }
        return "redirect:/parent/shop";
    }

    /**
     * Обновить товар в магазине.
     */
    @PostMapping("/shop/{id}/edit")
    public String editShopItem(
            @PathVariable UUID id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam int priceCoins,
            @RequestParam(required = false) String iconName,
            @RequestParam(required = false) List<UUID> childIds,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("title", title);
            request.put("description", description);
            request.put("priceCoins", priceCoins);
            if (iconName != null && !iconName.isBlank()) {
                request.put("iconName", iconName);
            }
            if (childIds != null && !childIds.isEmpty()) {
                request.put("childIds", childIds.stream().map(UUID::toString).toList());
            }

            userServiceClient.updateShopItem(id, request);
            redirectAttributes.addFlashAttribute("success", "Товар обновлён!");
        } catch (Exception e) {
            log.error("Error updating shop item: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления товара");
        }
        return "redirect:/parent/shop";
    }

    /**
     * Одобрить покупку.
     */
    @PostMapping("/shop/purchases/{id}/approve")
    public String approvePurchase(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.approvePurchase(id, comment);
            redirectAttributes.addFlashAttribute("success", "Покупка одобрена!");
        } catch (Exception e) {
            log.error("Error approving purchase: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Ошибка одобрения";
            redirectAttributes.addFlashAttribute("error", msg);
        }
        return "redirect:/parent/shop";
    }

    /**
     * Отклонить покупку.
     */
    @PostMapping("/shop/purchases/{id}/reject")
    public String rejectPurchase(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.rejectPurchase(id, comment);
            redirectAttributes.addFlashAttribute("success", "Покупка отклонена");
        } catch (Exception e) {
            log.error("Error rejecting purchase: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Ошибка отклонения";
            redirectAttributes.addFlashAttribute("error", msg);
        }
        return "redirect:/parent/shop";
    }

    // ==================== Level Rewards ====================

    /**
     * Страница управления наградами за уровни.
     */
    @GetMapping("/level-rewards")
    public String levelRewards(@RequestParam(required = false) UUID childId,
                               @RequestParam(required = false) boolean onboarding,
                               Model model) {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            model.addAttribute("children", children);
            model.addAttribute("onboarding", onboarding);

            // Auto-select first child if only one, or use provided childId
            ChildDto selectedChild = null;
            if (childId != null) {
                selectedChild = children.stream()
                        .filter(c -> c.id().equals(childId))
                        .findFirst().orElse(null);
            } else if (children.size() == 1) {
                selectedChild = children.get(0);
            }

            if (selectedChild != null) {
                model.addAttribute("selectedChild", selectedChild);

                List<LevelRewardDto> rewards = userServiceClient.getLevelRewards(selectedChild.id());
                model.addAttribute("rewards", rewards);

                Set<Integer> rewardedLevels = rewards.stream()
                        .map(LevelRewardDto::level)
                        .collect(Collectors.toSet());
                model.addAttribute("rewardedLevels", rewardedLevels);

                // Determine range for level info: from next unrewarded to +10
                int maxRewarded = rewards.stream().mapToInt(LevelRewardDto::level).max().orElse(1);
                int from = Math.max(2, selectedChild.level() + 1);
                int to = Math.max(maxRewarded + 10, from + 9);
                List<LevelInfoDto> levelInfoList = userServiceClient.getLevelInfo(selectedChild.id(), from, to);
                model.addAttribute("levelInfoList", levelInfoList);

                List<ShopItemDto> shopItems = userServiceClient.getShopItems();
                model.addAttribute("shopItems", shopItems);
            }
        } catch (Exception e) {
            log.error("Error loading level rewards: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки наград за уровни");
        }
        return "parent/level-rewards";
    }

    /**
     * Добавить награды за уровни.
     */
    @PostMapping("/level-rewards/{childId}/add")
    public String addLevelRewards(
            @PathVariable UUID childId,
            @RequestParam List<Integer> levels,
            @RequestParam List<String> titles,
            @RequestParam List<String> descriptions,
            @RequestParam List<String> shopItemIds,
            RedirectAttributes redirectAttributes
    ) {
        try {
            List<Map<String, Object>> rewardsList = new ArrayList<>();
            for (int i = 0; i < levels.size(); i++) {
                String title = i < titles.size() ? titles.get(i) : "";
                if (title == null || title.isBlank()) continue;

                Map<String, Object> reward = new HashMap<>();
                reward.put("level", levels.get(i));
                reward.put("title", title);
                String desc = i < descriptions.size() ? descriptions.get(i) : "";
                if (desc != null && !desc.isBlank()) {
                    reward.put("description", desc);
                }
                String shopItemId = i < shopItemIds.size() ? shopItemIds.get(i) : "";
                if (shopItemId != null && !shopItemId.isBlank()) {
                    reward.put("shopItemId", shopItemId);
                }
                rewardsList.add(reward);
            }

            if (rewardsList.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Заполните хотя бы одну награду");
                return "redirect:/parent/level-rewards?childId=" + childId;
            }

            Map<String, Object> request = new HashMap<>();
            request.put("rewards", rewardsList);
            userServiceClient.createLevelRewards(childId, request);

            redirectAttributes.addFlashAttribute("success", "Награды сохранены!");
        } catch (Exception e) {
            log.error("Error adding level rewards: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка сохранения наград");
        }
        return "redirect:/parent/level-rewards?childId=" + childId;
    }

    /**
     * Отметить награду как выданную.
     */
    @PostMapping("/level-rewards/{id}/issued")
    public String markRewardIssued(
            @PathVariable UUID id,
            @RequestParam UUID childId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.markRewardIssued(id);
            redirectAttributes.addFlashAttribute("success", "Награда отмечена как выданная");
        } catch (Exception e) {
            log.error("Error marking reward issued: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления награды");
        }
        return "redirect:/parent/level-rewards?childId=" + childId;
    }

    /**
     * Удалить награду за уровень.
     */
    @PostMapping("/level-rewards/{id}/delete")
    public String deleteLevelReward(
            @PathVariable UUID id,
            @RequestParam UUID childId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.deleteLevelReward(id);
            redirectAttributes.addFlashAttribute("success", "Награда удалена");
        } catch (Exception e) {
            log.error("Error deleting level reward: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления награды");
        }
        return "redirect:/parent/level-rewards?childId=" + childId;
    }

    // ==================== Marketplace ====================

    /**
     * Страница маркетплейса готовых наград.
     */
    @GetMapping("/shop/marketplace")
    public String marketplace(@RequestParam(required = false) boolean onboarding,
                              Model model) {
        try {
            List<ShopItemDto> marketplaceItems = userServiceClient.getMarketplaceItems();
            List<ChildDto> children = userServiceClient.getChildren();
            model.addAttribute("marketplaceItems", marketplaceItems);
            model.addAttribute("children", children);
            model.addAttribute("onboarding", onboarding);
        } catch (Exception e) {
            log.error("Error loading marketplace: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки маркетплейса");
        }
        return "parent/marketplace";
    }

    /**
     * Добавить товар из маркетплейса в свой магазин.
     */
    @PostMapping("/shop/marketplace/{id}/copy")
    public String copyFromMarketplace(
            @PathVariable UUID id,
            @RequestParam List<UUID> childIds,
            @RequestParam(required = false) boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("childIds", childIds.stream().map(UUID::toString).toList());
            userServiceClient.copyFromMarketplace(id, request);
            redirectAttributes.addFlashAttribute("success", "Награда добавлена в ваш магазин!");
        } catch (Exception e) {
            log.error("Error copying from marketplace: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Ошибка добавления награды";
            redirectAttributes.addFlashAttribute("error", msg);
        }
        return "redirect:/parent/shop/marketplace" + (onboarding ? "?onboarding=true" : "");
    }
}
