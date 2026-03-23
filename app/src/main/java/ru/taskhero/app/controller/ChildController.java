package ru.taskhero.app.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.taskhero.app.client.TaskServiceClient;
import ru.taskhero.app.client.UserServiceClient;
import ru.taskhero.app.dto.ChildDto;
import ru.taskhero.app.dto.LevelRewardDto;
import ru.taskhero.app.dto.ShopItemDto;
import ru.taskhero.app.dto.ShopPurchaseDto;
import ru.taskhero.app.dto.TaskAssignmentDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер для страниц ребёнка.
 */
@Slf4j
@Controller
@RequestMapping("/child")
@RequiredArgsConstructor
public class ChildController {

    private final TaskServiceClient taskServiceClient;
    private final UserServiceClient userServiceClient;

    /**
     * Dashboard ребёнка.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Проверяем, выбран ли персонаж
            ChildDto childProfile = userServiceClient.getMyChildProfile();
            if (!childProfile.characterSelected()) {
                return "redirect:/child/select-character";
            }
            model.addAttribute("childProfile", childProfile);

            List<TaskAssignmentDto> activeAssignments = taskServiceClient.getMyActiveAssignments();
            List<TaskAssignmentDto> allAssignments = taskServiceClient.getMyAssignments(null);

            // Подсчёт статистики
            long completedCount = allAssignments.stream()
                    .filter(a -> "APPROVED".equals(a.status()))
                    .count();
            long pendingCount = activeAssignments.size();

            int totalExp = allAssignments.stream()
                    .filter(a -> a.expEarned() != null)
                    .mapToInt(TaskAssignmentDto::expEarned)
                    .sum();
            int totalCoins = allAssignments.stream()
                    .filter(a -> a.coinsEarned() != null)
                    .mapToInt(TaskAssignmentDto::coinsEarned)
                    .sum();

            model.addAttribute("activeAssignments", activeAssignments);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("totalExp", totalExp);
            model.addAttribute("totalCoins", totalCoins);

            // Load unseen level rewards
            try {
                List<LevelRewardDto> unseenRewards = userServiceClient.getUnseenRewards(childProfile.id());
                model.addAttribute("unseenRewards", unseenRewards);
            } catch (Exception ex) {
                log.warn("Error loading unseen rewards: {}", ex.getMessage());
            }

            // Calculate EXP remaining to next level
            int expRemaining = childProfile.nextLevelExp() - childProfile.currentLevelExp();
            model.addAttribute("expRemaining", Math.max(expRemaining, 0));

            // Load next unclaimed reward
            try {
                List<LevelRewardDto> allRewards = userServiceClient.getLevelRewards(childProfile.id());
                LevelRewardDto nextReward = allRewards.stream()
                        .filter(r -> !r.claimed() && r.level() > childProfile.level())
                        .findFirst()
                        .orElse(null);
                model.addAttribute("nextReward", nextReward);
            } catch (Exception ex) {
                log.warn("Error loading next reward: {}", ex.getMessage());
            }

        } catch (Exception e) {
            log.error("Error loading child dashboard: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки данных");
        }

        return "child/dashboard";
    }

    /**
     * Страница выбора персонажа (первый вход).
     */
    @GetMapping("/select-character")
    public String selectCharacterPage(Model model) {
        try {
            ChildDto childProfile = userServiceClient.getMyChildProfile();
            if (childProfile.characterSelected()) {
                return "redirect:/child/dashboard";
            }
            model.addAttribute("childProfile", childProfile);
        } catch (Exception e) {
            log.error("Error loading character selection: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки");
        }
        return "child/select-character";
    }

    /**
     * Выбрать персонажа.
     */
    @PostMapping("/select-character")
    public String selectCharacter(
            @RequestParam String characterType,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.selectCharacter(characterType);
            redirectAttributes.addFlashAttribute("success", "Персонаж выбран!");
        } catch (Exception e) {
            log.error("Error selecting character: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка выбора персонажа");
            return "redirect:/child/select-character";
        }
        return "redirect:/child/dashboard";
    }

    /**
     * Мои задания.
     */
    @GetMapping("/tasks")
    public String myTasks(@RequestParam(required = false) String status, Model model) {
        try {
            List<TaskAssignmentDto> assignments = taskServiceClient.getMyAssignments(status);
            model.addAttribute("assignments", assignments);
            model.addAttribute("currentStatus", status);
        } catch (Exception e) {
            log.error("Error loading tasks: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки заданий");
        }
        return "child/tasks";
    }

    /**
     * Сдать задание.
     */
    @PostMapping("/tasks/{id}/submit")
    public String submitTask(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> request = new HashMap<>();
            if (comment != null && !comment.isEmpty()) {
                request.put("comment", comment);
            }

            taskServiceClient.submitTask(id, request);

            redirectAttributes.addFlashAttribute("success", "Задание сдано на проверку!");

        } catch (Exception e) {
            log.error("Error submitting task: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка сдачи задания");
        }

        return "redirect:/child/dashboard";
    }

    /**
     * История заданий.
     */
    @GetMapping("/history")
    public String history(Model model) {
        try {
            List<TaskAssignmentDto> allAssignments = taskServiceClient.getMyAssignments(null);

            // Фильтруем только завершённые (APPROVED или REJECTED)
            List<TaskAssignmentDto> completedAssignments = allAssignments.stream()
                    .filter(a -> "APPROVED".equals(a.status()) || "REJECTED".equals(a.status()))
                    .toList();

            model.addAttribute("assignments", completedAssignments);

        } catch (Exception e) {
            log.error("Error loading history: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки истории");
        }

        return "child/history";
    }

    // ==================== Level Rewards ====================

    /**
     * Отметить награду как просмотренную.
     */
    @PostMapping("/rewards/{id}/seen")
    public String markRewardSeen(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.markRewardSeen(id);
        } catch (Exception e) {
            log.warn("Error marking reward seen: {}", e.getMessage());
        }
        return "redirect:/child/dashboard";
    }

    // ==================== Shop ====================

    /**
     * Магазин ребёнка — просмотр доступных товаров.
     */
    @GetMapping("/shop")
    public String shop(Model model) {
        try {
            List<ShopItemDto> availableItems = userServiceClient.getAvailableShopItems();
            List<ShopPurchaseDto> myPurchases = userServiceClient.getMyPurchases();

            model.addAttribute("items", availableItems);
            model.addAttribute("purchases", myPurchases);

            // Подгрузка баланса
            try {
                ChildDto profile = userServiceClient.getMyChildProfile();
                model.addAttribute("childProfile", profile);
            } catch (Exception ex) {
                log.warn("Could not load child profile for shop: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("Error loading child shop: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки магазина");
        }
        return "child/shop";
    }

    /**
     * Запросить покупку товара.
     */
    @PostMapping("/shop/buy/{itemId}")
    public String requestPurchase(
            @PathVariable UUID itemId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.requestPurchase(itemId);
            redirectAttributes.addFlashAttribute("success", "Запрос на покупку отправлен!");
        } catch (Exception e) {
            log.error("Error requesting purchase: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка запроса покупки. Возможно, недостаточно монет.");
        }
        return "redirect:/child/shop";
    }
}
