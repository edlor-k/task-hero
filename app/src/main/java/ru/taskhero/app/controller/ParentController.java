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
import ru.taskhero.app.security.ChildSessionStarter;
import ru.taskhero.app.util.FormDateTimeParser;

import java.time.Instant;
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
    private final ChildSessionStarter childSessionStarter;

    /**
     * Добавляет атрибут onboardingComplete для всех страниц родителя.
     * Используется для скрытия пункта меню «Онбординг» после завершения настройки.
     * <p>
     * Онбординг считается пройденным, как только есть ребёнок и ему назначено хотя бы
     * одно задание — этого достаточно, чтобы семья начала пользоваться основным циклом.
     * Магазин наград и награды за уровни — необязательные дополнения, а не обязательные
     * подсистемы для запуска (см. п.13 продуктового ТЗ).
     */
    @ModelAttribute("onboardingComplete")
    public boolean isOnboardingComplete() {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            if (children.isEmpty()) {
                return false;
            }
            return hasAnyAssignmentEver();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Есть ли у родителя хотя бы одно когда-либо назначенное задание.
     */
    private boolean hasAnyAssignmentEver() {
        try {
            Map<String, Object> page = taskServiceClient.getAssignments(0, 1);
            Object totalElements = page.get("totalElements");
            return totalElements instanceof Number && ((Number) totalElements).longValue() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @ModelAttribute("sidebarPendingReviewCount")
    public int sidebarPendingReviewCount() {
        try {
            return taskServiceClient.getPendingReview().size();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarPendingPurchaseCount")
    public int sidebarPendingPurchaseCount() {
        try {
            return userServiceClient.getPendingPurchases().size();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarExpiredAssignmentCount")
    public int sidebarExpiredAssignmentCount() {
        try {
            return taskServiceClient.getExpiredAssignments().size();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarRewardAlertCount")
    public int sidebarRewardAlertCount() {
        try {
            int count = 0;
            for (ChildDto child : userServiceClient.getChildren()) {
                Map<String, Integer> unfilledInfo = userServiceClient.getUnfilledCount(child.id());
                if (unfilledInfo.getOrDefault("unfilledCount", 0) < 5) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Страница онбординга для новых родителей: два шага — добавить ребёнка,
     * назначить первое задание. Магазин наград и награды за уровни в онбординг
     * не входят — это необязательные дополнения, а не обязательные подсистемы.
     */
    @GetMapping("/onboarding")
    public String onboarding(Model model) {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            boolean hasChildren = !children.isEmpty();
            boolean hasFirstAssignment = hasChildren && hasAnyAssignmentEver();

            model.addAttribute("hasChildren", hasChildren);
            model.addAttribute("hasFirstAssignment", hasFirstAssignment);
            model.addAttribute("children", children);
        } catch (Exception e) {
            log.error("Error loading onboarding: {}", e.getMessage());
            model.addAttribute("hasChildren", false);
            model.addAttribute("hasFirstAssignment", false);
        }
        return "parent/onboarding";
    }

    /**
     * Финальный шаг онбординга: первое задание назначено, приложение готово
     * передавать ребёнку.
     */
    @GetMapping("/onboarding/done")
    public String onboardingDone(Model model) {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            model.addAttribute("child", children.isEmpty() ? null : children.get(children.size() - 1));
        } catch (Exception e) {
            log.error("Error loading onboarding done page: {}", e.getMessage());
        }
        return "parent/onboarding-done";
    }

    /**
     * Dashboard родителя.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            // Родитель без единого ребёнка ещё не может пройти основной цикл — вместо
            // пустого дашборда сразу направляем в сценарий добавления первого ребёнка.
            if (children.isEmpty()) {
                return "redirect:/parent/onboarding";
            }
            List<TaskAssignmentDto> pendingReview = taskServiceClient.getPendingReview();
            List<TaskTemplateDto> templates = taskServiceClient.getActiveTemplates();

            model.addAttribute("children", children);
            model.addAttribute("pendingReview", pendingReview);
            model.addAttribute("pendingCount", pendingReview.size());
            model.addAttribute("templates", templates);

            // Онбординг считается неполным, пока нет ни ребёнка, ни первого назначенного
            // задания — магазин наград и награды за уровни для запуска не обязательны.
            boolean hasChildren = !children.isEmpty();
            boolean onboardingIncomplete = !hasChildren || !hasAnyAssignmentEver();
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
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) List<String> presetGroups,
            @RequestParam(defaultValue = "false") boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String trimmedNickname = (nickname != null && !nickname.isBlank()) ? nickname.trim() : null;
            CreateChildRequest request = new CreateChildRequest(firstName, surname, "NORMAL", trimmedNickname);
            ChildDto child = userServiceClient.createChild(request);

            int presetsApplied = 0;
            int tasksApplied = 0;
            if (presetGroups != null && !presetGroups.isEmpty()) {
                for (String presetGroup : presetGroups) {
                    // Применяем пресет наград (shop items)
                    try {
                        Map<String, Object> result = userServiceClient.applyPreset(presetGroup, child.id());
                        Object added = result.get("added");
                        if (added instanceof Number n) presetsApplied += n.intValue();
                    } catch (Exception ex) {
                        log.warn("Failed to apply reward preset {} for child {}: {}", presetGroup, child.id(), ex.getMessage());
                    }
                    // Применяем пресет заданий — создаём шаблоны в библиотеке родителя
                    List<Map<String, Object>> presetTasks = buildPresetTasks(normalizePresetGroup(presetGroup));
                    if (presetTasks != null) {
                        for (Map<String, Object> task : presetTasks) {
                            try {
                                taskServiceClient.createTemplate(task);
                                tasksApplied++;
                            } catch (Exception ex) {
                                log.warn("Failed to create preset task '{}': {}", task.get("title"), ex.getMessage());
                            }
                        }
                    }
                }
            }

            String msg = "Ребёнок добавлен! Токен для входа: " + child.loginToken();
            if (presetsApplied > 0 || tasksApplied > 0) {
                msg += ". Из пресетов добавлено: " + presetsApplied + " наград, " + tasksApplied + " шаблонов заданий.";
            }
            redirectAttributes.addFlashAttribute("success", msg);

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
     * Открыть кабинет ребёнка прямо с устройства родителя — ребёнку не нужно
     * переписывать длинный технический токен на своём устройстве. Работает только
     * для детей текущего родителя. Токен входа никогда не попадает в браузер.
     */
    @PostMapping("/children/{id}/open-cabinet")
    public String openChildCabinet(
            @PathVariable UUID id,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ChildDto child = userServiceClient.getChildren().stream()
                    .filter(c -> c.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Ребёнок не найден"));

            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("loginToken", child.loginToken());
            ru.taskhero.app.dto.LoginResponse loginResponse = userServiceClient.loginChild(loginRequest);

            childSessionStarter.start(loginResponse, request, response);
            log.info("Parent opened child cabinet for child {}", id);

            return "redirect:/child/dashboard";
        } catch (Exception e) {
            log.error("Error opening child cabinet for {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Не удалось открыть кабинет ребёнка");
            return "redirect:/parent/children";
        }
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
    public String templates(
            @RequestParam(required = false) boolean onboarding,
            @RequestParam(required = false) String category,
            Model model
    ) {
        try {
            Map<String, Object> response = taskServiceClient.getTemplates(0, 50, category);
            model.addAttribute("templates", response.get("content"));
            model.addAttribute("onboarding", onboarding);
            model.addAttribute("selectedCategory", category);
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
            @RequestParam(defaultValue = "false") boolean autoSubmit,
            @RequestParam(defaultValue = "false") boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = buildTemplateRequest(title, description, expReward, coinsReward,
                    repeatable, repeatCount, repeatInterval, repeatUnit,
                    difficulty, category, recurrenceRule, subItems);
            request.put("autoSubmit", autoSubmit);

            taskServiceClient.createTemplate(request);

            redirectAttributes.addFlashAttribute("success", "Шаблон создан!");

        } catch (Exception e) {
            log.error("Error creating template: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Ошибка создания шаблона";
            redirectAttributes.addFlashAttribute("error", msg);
        }

        return "redirect:/parent/templates" + (onboarding ? "?onboarding=true" : "");
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
            @RequestParam(defaultValue = "false") boolean autoSubmit,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("title", title);
            request.put("description", description);
            request.put("expReward", expReward);
            request.put("coinsReward", coinsReward);
            request.put("difficulty", difficulty);
            request.put("autoSubmit", autoSubmit);
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
        if (onboarding) {
            redirect.append(sep).append("onboarding=true");
            sep = "&";
        }
        if (category != null && !category.isBlank()) {
            redirect.append(sep).append("category=").append(category);
        }
        return redirect.toString();
    }

    // ==================== Presets ====================

    @PostMapping("/presets/apply")
    public String applyPreset(
            @RequestParam String presetName,
            @RequestParam(defaultValue = "false") boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        List<Map<String, Object>> tasks = buildPresetTasks(presetName);
        if (tasks == null) {
            redirectAttributes.addFlashAttribute("error", "Пресет не найден");
            return "redirect:/parent/library" + (onboarding ? "?onboarding=true" : "");
        }
        int created = 0;
        for (Map<String, Object> task : tasks) {
            try {
                taskServiceClient.createTemplate(task);
                created++;
            } catch (Exception e) {
                log.warn("Preset task creation failed: {}", e.getMessage());
            }
        }
        redirectAttributes.addFlashAttribute("success", "Пресет применён: добавлено " + created + " заданий");
        return "redirect:/parent/library" + (onboarding ? "?onboarding=true" : "");
    }

    /**
     * Нормализует название пресета из формы (STUDY, SPORTS, CLEAN, ROUTINE, HOBBY, GAMER)
     * в ключ, используемый {@link #buildPresetTasks}.
     */
    private static String normalizePresetGroup(String presetGroup) {
        if (presetGroup == null) return null;
        return switch (presetGroup.toUpperCase()) {
            case "CLEAN" -> "household";
            default -> presetGroup.toLowerCase();
        };
    }

    private List<Map<String, Object>> buildPresetTasks(String presetName) {
        return switch (presetName) {
            case "study" -> List.of(
                presetTask("Сделать домашнее задание", "Выполнить все задания на день", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 20, 20, "STUDY"),
                presetTask("Собрать портфель на завтра", "Проверить тетради, учебники, форму", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 3, 3, "STUDY"),
                presetTask("Прочитать 10 страниц", "Любая книга: школьная или художественная", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 5, 5, "READING"),
                presetTask("Уборка рабочего места", "Разложить всё после занятий на свои места", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 5, 5, "STUDY")
            );
            case "sports" -> List.of(
                presetTask("Утренняя зарядка", "5–10 минут простой разминки", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 10, 10, "SPORTS"),
                presetTask("Прогулка / 5000 шагов", "Любая активная прогулка, шаги", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 15, 15, "SPORTS"),
                presetTask("Тренировка", "Секция, бассейн, футбол, танцы или домашний спорт", "FREQ=WEEKLY;BYDAY=MO,WE,FR", 20, 20, "SPORTS"),
                presetTask("Растяжка", "5–10 минут после активности", "FREQ=WEEKLY;BYDAY=MO,TU,TH,SA", 5, 5, "SPORTS"),
                presetTask("Мини-челлендж", "10 отжиманий / 20 приседаний / планка", "FREQ=WEEKLY;BYDAY=MO,TU,TH,SA", 5, 5, "SPORTS")
            );
            case "household" -> List.of(
                presetTask("Застелить кровать", "Сразу после подъёма", "FREQ=DAILY", 5, 5, "HOUSEHOLD"),
                presetTask("Убрать игрушки / вещи", "Убрать за собой после игры", "FREQ=DAILY", 5, 5, "HOUSEHOLD"),
                presetTask("Протереть стол после занятий", "Убрать стол после уроков/творчества", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 5, 5, "HOUSEHOLD"),
                presetTask("Помочь накрыть / убрать со стола", "Любая помощь на кухне", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 10, 10, "HOUSEHOLD"),
                presetTask("Убрать свою комнату", "Полноценная уборка комнаты", "FREQ=WEEKLY;BYDAY=TU,FR", 20, 20, "HOUSEHOLD")
            );
            case "routine" -> List.of(
                presetTask("Почистить зубы утром и вечером", "Выполнить оба раза за день", "FREQ=DAILY", 5, 5, "HYGIENE"),
                presetTask("Собраться утром без напоминаний", "Одеться, умыться, подготовиться", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 10, 10, "HYGIENE"),
                presetTask("Лечь спать вовремя", "По времени, которое задаёт родитель", "FREQ=DAILY", 5, 5, "HYGIENE"),
                presetTask("Собрать вещи на завтра", "Одежда / рюкзак / нужные вещи", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 7, 7, "HYGIENE")
            );
            case "hobby" -> List.of(
                presetTask("20 минут любимого хобби", "Рисование, музыка, конструктор, лепка, шахматы и т.д.", "FREQ=WEEKLY;BYDAY=MO,TU,TH,SA", 15, 15, "CREATIVITY"),
                presetTask("Практика / репетиция", "Целенаправленно потренироваться", "FREQ=WEEKLY;BYDAY=MO,WE,FR", 20, 20, "CREATIVITY"),
                presetTask("Закончить маленький проект", "Доделать рисунок, поделку, мелодию, модель", "FREQ=WEEKLY", 25, 25, "CREATIVITY"),
                presetTask("Попробовать что-то новое", "Новый материал, техника, формат", "FREQ=WEEKLY", 25, 25, "CREATIVITY")
            );
            case "gamer" -> List.of(
                presetTask("Выполнить дневное задание в игре", "Любая игра: квест, ежедневная миссия, достижение", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", 10, 10, "CREATIVITY"),
                presetTask("Пройти новый уровень / глава", "Продвинуться по сюжету или кампании", "FREQ=WEEKLY;BYDAY=SA,SU", 20, 20, "CREATIVITY"),
                presetTask("Поиграть с другом или семьёй", "Совместная онлайн или локальная игра", "FREQ=WEEKLY;BYDAY=SA,SU", 15, 15, "CREATIVITY"),
                presetTask("Сделать перерыв после часа игры", "Встать, размяться, попить воды", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR,SA,SU", 5, 5, "HYGIENE")
            );
            default -> null;
        };
    }

    private Map<String, Object> presetTask(String title, String description, String rrule,
                                            int expReward, int coinsReward, String category) {
        Map<String, Object> t = new HashMap<>();
        t.put("title", title);
        t.put("description", description);
        t.put("recurrenceRule", rrule);
        t.put("expReward", expReward);
        t.put("coinsReward", coinsReward);
        t.put("category", category);
        t.put("repeatable", true);
        t.put("difficulty", "NORMAL");
        return t;
    }

    // ==================== Assignments ====================

    /**
     * Страница назначения задания.
     */
    @GetMapping("/assign")
    public String assignPage(@RequestParam(required = false) boolean onboarding, Model model) {
        try {
            List<ChildDto> children = userServiceClient.getChildren();
            List<TaskTemplateDto> templates = taskServiceClient.getActiveTemplates();

            model.addAttribute("children", children);
            model.addAttribute("templates", templates);
            // Если у родителя всего один ребёнок — выбирать его каждый раз не нужно,
            // форма подставит его автоматически и скроет селектор.
            model.addAttribute("singleChild", children.size() == 1 ? children.get(0) : null);
            model.addAttribute("onboarding", onboarding);

        } catch (Exception e) {
            log.error("Error loading assign page: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки данных");
        }

        return "parent/assign";
    }

    /**
     * Назначить задание.
     * <p>
     * Один путь для обеих исходных точек: выбор готового задания в форме — это лишь
     * клиентское автозаполнение полей ниже (см. assign.html), а не отдельная ветка
     * бизнес-логики. Если {@code templateId} передан — используем существующий шаблон
     * как есть; если нет (родитель создаёт своё задание или изменил предзаполненные
     * поля) — task-service создаст новый шаблон и назначение атомарно, одной
     * транзакцией (см. {@code TaskServiceClient#assignTaskWithTemplate}) — при любой
     * ошибке (в т.ч. в валидации самого назначения) не останется осиротевшего шаблона.
     */
    @PostMapping("/assign")
    public String assignTask(
            @RequestParam(required = false) UUID childId,
            @RequestParam(required = false) String templateId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) String recurrenceRule,
            @RequestParam(defaultValue = "5") int coinsReward,
            @RequestParam(defaultValue = "false") boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        String redirectBack = "redirect:/parent/assign" + (onboarding ? "?onboarding=true" : "");
        Map<String, Object> enteredValues = buildFormValuesEcho(
                childId, templateId, title, description, dueDate, recurrenceRule, coinsReward);

        UUID finalChildId = childId;
        if (finalChildId == null) {
            try {
                List<ChildDto> children = userServiceClient.getChildren();
                if (children.size() == 1) {
                    finalChildId = children.get(0).id();
                }
            } catch (Exception ex) {
                log.warn("Error loading children while assigning task: {}", ex.getMessage());
            }
        }
        if (finalChildId == null) {
            redirectAttributes.addFlashAttribute("error", "Выберите ребёнка");
            redirectAttributes.addFlashAttribute("formValues", enteredValues);
            return redirectBack;
        }

        UUID finalTemplateId;
        try {
            finalTemplateId = (templateId != null && !templateId.isBlank()) ? UUID.fromString(templateId) : null;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", "Некорректное задание");
            redirectAttributes.addFlashAttribute("formValues", enteredValues);
            return redirectBack;
        }

        if (finalTemplateId == null && (title == null || title.isBlank())) {
            redirectAttributes.addFlashAttribute("error", "Введите название задания или выберите готовое");
            redirectAttributes.addFlashAttribute("formValues", enteredValues);
            return redirectBack;
        }

        // Валидируем дедлайн ДО любого сетевого вызова: раньше шаблон уже мог быть
        // сохранён в task-service к этому моменту, и ошибка парсинга дедлайна
        // оставляла его осиротевшим (регрессия «Ошибка назначения задания»).
        Instant parsedDueDate;
        try {
            parsedDueDate = FormDateTimeParser.parseToInstant(dueDate);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", "Некорректный формат дедлайна");
            redirectAttributes.addFlashAttribute("formValues", enteredValues);
            return redirectBack;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("childId", finalChildId.toString());
        if (parsedDueDate != null) {
            request.put("dueDate", parsedDueDate.toString());
        }
        if (finalTemplateId != null) {
            request.put("templateId", finalTemplateId.toString());
        } else {
            // Своё задание (с нуля или изменённое после выбора готового) — task-service
            // создаст шаблон и назначение в одной транзакции. EXP — системная величина,
            // родитель её не задаёт, task-service применит разумное значение по умолчанию.
            request.put("title", title);
            request.put("description", description);
            request.put("coinsReward", coinsReward);
            if (recurrenceRule != null && !recurrenceRule.isBlank()) {
                request.put("recurrenceRule", recurrenceRule);
            }
        }

        try {
            taskServiceClient.assignTaskWithTemplate(request);
        } catch (Exception e) {
            log.error("Error assigning task: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractErrorMessage(e, "Ошибка назначения задания"));
            redirectAttributes.addFlashAttribute("formValues", enteredValues);
            return redirectBack;
        }

        try {
            Map<String, String> audit = new HashMap<>();
            audit.put("action", "TASK_ASSIGNED");
            audit.put("targetType", "ASSIGNMENT");
            audit.put("details", "Назначено задание ребёнку " + finalChildId);
            adminServiceClient.createAuditEntry(audit);
        } catch (Exception ex) {
            log.warn("Audit log failed: {}", ex.getMessage());
        }

        redirectAttributes.addFlashAttribute("success", "Задание назначено!");

        if (onboarding) {
            return "redirect:/parent/onboarding/done";
        }
        return "redirect:/parent/dashboard";
    }

    /**
     * Собрать введённые в форму назначения значения для повторного показа при ошибке —
     * без этого редирект на форму с флеш-ошибкой стирал всё, что родитель уже заполнил.
     */
    private Map<String, Object> buildFormValuesEcho(
            UUID childId, String templateId, String title, String description,
            String dueDate, String recurrenceRule, int coinsReward
    ) {
        Map<String, Object> values = new HashMap<>();
        values.put("childId", childId != null ? childId.toString() : null);
        values.put("templateId", templateId);
        values.put("title", title);
        values.put("description", description);
        values.put("recurrenceRule", recurrenceRule);
        values.put("coinsReward", coinsReward);
        values.put("dueDateLocal", toDatetimeLocalValue(dueDate));
        return values;
    }

    /**
     * Лучшее приближение для повторного отображения дедлайна в {@code <input type="datetime-local">}:
     * если то, что ввёл родитель, удаётся разобрать — приводим обратно к формату
     * {@code yyyy-MM-dd'T'HH:mm} в зоне сервера; иначе оставляем поле пустым (браузер
     * всё равно не примет невалидное значение в этом типе поля).
     */
    private String toDatetimeLocalValue(String rawDueDate) {
        try {
            Instant parsed = FormDateTimeParser.parseToInstant(rawDueDate);
            if (parsed == null) {
                return null;
            }
            return java.time.LocalDateTime.ofInstant(parsed, java.time.ZoneId.systemDefault())
                    .truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
                    .toString();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Извлечь читаемое сообщение об ошибке из тела ответа Feign (если оно есть),
     * чтобы не показывать родителю обезличенное сообщение вместо настоящей причины.
     */
    private String extractErrorMessage(Exception e, String fallback) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("\"message\":\"")) {
            int start = msg.indexOf("\"message\":\"") + 11;
            int end = msg.indexOf('"', start);
            if (end > start) {
                return msg.substring(start, end);
            }
        }
        return fallback;
    }

    /**
     * Страница проверки заданий.
     */
    @GetMapping("/review")
    public String reviewPage(Model model) {
        try {
            List<TaskAssignmentDto> pendingReview = taskServiceClient.getPendingReview();
            model.addAttribute("assignments", pendingReview);

            // TaskAssignmentDto хранит только childId — подтягиваем детей родителя,
            // чтобы шаблон мог показать имя/аватар ребёнка по каждому заданию.
            Map<UUID, ChildDto> childrenById = userServiceClient.getChildren().stream()
                    .collect(Collectors.toMap(ChildDto::id, c -> c));
            model.addAttribute("childrenById", childrenById);
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
            @RequestParam(required = false) UUID childId,
            @RequestParam(required = false, defaultValue = "dashboard") String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            taskServiceClient.deleteAssignment(id);
            redirectAttributes.addFlashAttribute("success", "Задание удалено");
        } catch (Exception e) {
            log.error("Error deleting assignment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractMessage(e, "Ошибка удаления задания"));
        }
        if (childId != null) {
            return "redirect:/parent/children/" + childId;
        }
        return switch (redirectTo) {
            case "review" -> "redirect:/parent/review";
            default -> "redirect:/parent/dashboard";
        };
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
            @RequestParam(required = false) String iconName,
            @RequestParam List<UUID> childIds,
            @RequestParam(defaultValue = "false") boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("title", title);
            request.put("description", description);
            request.put("priceCoins", priceCoins);
            request.put("iconName", normalizeRewardIcon(iconName));
            request.put("childIds", childIds.stream().map(UUID::toString).toList());

            userServiceClient.createShopItem(request);
            redirectAttributes.addFlashAttribute("success", "Товар добавлен!");
        } catch (Exception e) {
            log.error("Error adding shop item: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка добавления товара");
        }
        return "redirect:/parent/shop" + (onboarding ? "?onboarding=true" : "");
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
            request.put("iconName", normalizeRewardIcon(iconName));
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

                int maxRewarded = rewards.stream().mapToInt(LevelRewardDto::level).max().orElse(1);
                int from = Math.max(2, Math.max(selectedChild.level() + 1, maxRewarded + 1));
                int to = from + 1;
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
            @RequestParam(defaultValue = "false") boolean onboarding,
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
                return "redirect:/parent/level-rewards?childId=" + childId + (onboarding ? "&onboarding=true" : "");
            }

            Map<String, Object> request = new HashMap<>();
            request.put("rewards", rewardsList);
            userServiceClient.createLevelRewards(childId, request);

            redirectAttributes.addFlashAttribute("success", "Награды сохранены!");
        } catch (Exception e) {
            log.error("Error adding level rewards: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка сохранения наград");
        }
        return "redirect:/parent/level-rewards?childId=" + childId + (onboarding ? "&onboarding=true" : "");
    }

    /**
     * Отметить награду как выданную.
     */
    @PostMapping("/level-rewards/{id}/issued")
    public String markRewardIssued(
            @PathVariable UUID id,
            @RequestParam UUID childId,
            @RequestParam(defaultValue = "false") boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.markRewardIssued(id);
            redirectAttributes.addFlashAttribute("success", "Награда отмечена как выданная");
        } catch (Exception e) {
            log.error("Error marking reward issued: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления награды");
        }
        return "redirect:/parent/level-rewards?childId=" + childId + (onboarding ? "&onboarding=true" : "");
    }

    /**
     * Удалить награду за уровень.
     */
    @PostMapping("/level-rewards/{id}/delete")
    public String deleteLevelReward(
            @PathVariable UUID id,
            @RequestParam UUID childId,
            @RequestParam(defaultValue = "false") boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.deleteLevelReward(id);
            redirectAttributes.addFlashAttribute("success", "Награда удалена");
        } catch (Exception e) {
            log.error("Error deleting level reward: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления награды");
        }
        return "redirect:/parent/level-rewards?childId=" + childId + (onboarding ? "&onboarding=true" : "");
    }

    // ==================== Marketplace ====================

    private static final Map<String, String> PRESET_LABELS = Map.of(
            "STUDY", "Учёба",
            "SPORTS", "Спорт",
            "CLEAN", "Уборка",
            "ROUTINE", "Режим дня",
            "HOBBY", "Хобби",
            "GAMER", "Геймер"
    );

    /**
     * Страница маркетплейса готовых наград.
     */
    @GetMapping("/shop/marketplace")
    public String marketplace(@RequestParam(required = false) boolean onboarding,
                              Model model) {
        try {
            List<ShopItemDto> marketplaceItems = userServiceClient.getMarketplaceItems();
            List<ChildDto> children = userServiceClient.getChildren();

            // Group items by presetGroup; items without group go into ungroupedItems
            var grouped = new java.util.LinkedHashMap<String, List<ShopItemDto>>();
            var ungrouped = new java.util.ArrayList<ShopItemDto>();
            for (ShopItemDto item : marketplaceItems) {
                String pg = item.presetGroup();
                if (pg != null && !pg.isBlank()) {
                    grouped.computeIfAbsent(pg, k -> new java.util.ArrayList<>()).add(item);
                } else {
                    ungrouped.add(item);
                }
            }

            model.addAttribute("marketplaceItems", marketplaceItems);
            model.addAttribute("groupedItems", grouped);
            model.addAttribute("ungroupedItems", ungrouped);
            model.addAttribute("presetLabels", PRESET_LABELS);
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

    /**
     * Применить пресет наград: массово добавить все товары группы для ребёнка.
     */
    @PostMapping("/shop/preset/apply")
    public String applyShopPreset(
            @RequestParam String presetGroup,
            @RequestParam UUID childId,
            @RequestParam(required = false) boolean onboarding,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> result = userServiceClient.applyPreset(presetGroup, childId);
            int added = result.get("added") instanceof Number n ? n.intValue() : 0;
            redirectAttributes.addFlashAttribute("success",
                    "Пресет применён: " + added + " наград добавлено в ваш магазин!");
        } catch (Exception e) {
            log.error("Error applying shop preset: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    extractMessage(e, "Ошибка применения пресета"));
        }
        return "redirect:/parent/shop/marketplace" + (onboarding ? "?onboarding=true" : "");
    }

    private String normalizeRewardIcon(String iconName) {
        return iconName != null && !iconName.isBlank() ? iconName.trim() : "🎁";
    }

    private String extractMessage(Exception e, String fallback) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("\"message\":\"")) {
            int start = msg.indexOf("\"message\":\"") + 11;
            int end = msg.indexOf("\"", start);
            if (end > start) {
                return msg.substring(start, end);
            }
        }
        return msg != null ? msg : fallback;
    }
}
