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
import ru.taskhero.app.client.TaskServiceClient;
import ru.taskhero.app.client.UserServiceClient;
import ru.taskhero.app.dto.AvatarOptionDto;
import ru.taskhero.app.dto.BackgroundOptionDto;
import ru.taskhero.app.dto.ChildDto;
import ru.taskhero.app.dto.LevelRewardDto;
import ru.taskhero.app.dto.ShopItemDto;
import ru.taskhero.app.dto.ShopPurchaseDto;
import ru.taskhero.app.dto.TaskAssignmentDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @ModelAttribute("sidebarActiveTaskCount")
    public int sidebarActiveTaskCount() {
        try {
            return taskServiceClient.getMyActiveAssignments().size();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarPendingPurchaseCount")
    public int sidebarPendingPurchaseCount() {
        try {
            return (int) userServiceClient.getMyPurchases().stream()
                    .filter(p -> "PENDING".equals(p.status()))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarUnseenRewardCount")
    public int sidebarUnseenRewardCount() {
        try {
            ChildDto childProfile = userServiceClient.getMyChildProfile();
            return userServiceClient.getUnseenRewards(childProfile.id()).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Акцентный цвет ребёнка — доступен на всех страницах раздела /child для data-accent на body.
     */
    @ModelAttribute("childAccentColor")
    public String childAccentColor() {
        try {
            ChildDto profile = userServiceClient.getMyChildProfile();
            return profile.accentColor() != null ? profile.accentColor() : "primary";
        } catch (Exception e) {
            return "primary";
        }
    }

    /**
     * URL фона ребёнка — доступен на всех страницах раздела /child для инлайн-стиля th-content.
     */
    @ModelAttribute("childBackgroundUrl")
    public String childBackgroundUrl() {
        try {
            return userServiceClient.getMyChildProfile().backgroundUrl();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Баланс монет ребёнка для отображения в шапке — доступен на всех страницах раздела /child.
     */
    @ModelAttribute("topbarCoins")
    public int topbarCoins() {
        try {
            return userServiceClient.getMyChildProfile().coins();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Аватар ребёнка для отображения в шапке — доступен на всех страницах раздела /child.
     */
    @ModelAttribute("topbarAvatarUrl")
    public String topbarAvatarUrl() {
        try {
            return userServiceClient.getMyChildProfile().avatarUrl();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Dashboard ребёнка.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Проверяем, выбран ли аватар
            ChildDto childProfile = userServiceClient.getMyChildProfile();
            if (!childProfile.avatarSelected()) {
                return "redirect:/child/select-avatar";
            }
            model.addAttribute("childProfile", childProfile);

            List<TaskAssignmentDto> activeAssignments = taskServiceClient.getMyActiveAssignments();
            List<TaskAssignmentDto> allAssignments = taskServiceClient.getMyAssignments(null);

            // Подсчёт статистики
            long completedCount = allAssignments.stream()
                    .filter(a -> "APPROVED".equals(a.status()))
                    .count();
            long pendingCount = activeAssignments.size();

            int totalExp = childProfile.exp();
            int totalCoins = childProfile.coins();

            model.addAttribute("activeAssignments", activeAssignments);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("totalExp", totalExp);
            model.addAttribute("totalCoins", totalCoins);

            // Reassigned tasks: tasks that were extended after expiration
            List<TaskAssignmentDto> reassigned = activeAssignments.stream()
                    .filter(a -> "CREATED".equals(a.status()))
                    .filter(a -> Boolean.TRUE.equals(a.reassigned()))
                    .toList();
            model.addAttribute("reassignedAssignments", reassigned);

            // Recent results: recently reviewed tasks (approved/rejected)
            List<TaskAssignmentDto> recentResults = allAssignments.stream()
                    .filter(a -> "APPROVED".equals(a.status()) || "REJECTED".equals(a.status()))
                    .filter(a -> a.reviewedAt() != null
                            && a.reviewedAt().isAfter(java.time.Instant.now().minus(java.time.Duration.ofDays(3))))
                    .toList();
            model.addAttribute("recentResults", recentResults);

            // Recent purchases
            try {
                List<ShopPurchaseDto> myPurchases = userServiceClient.getMyPurchases();
                List<ShopPurchaseDto> recentPurchases = myPurchases.stream()
                        .filter(p -> "APPROVED".equals(p.status()) || "REJECTED".equals(p.status()))
                        .filter(p -> p.reviewedAt() != null
                                && p.reviewedAt().isAfter(java.time.Instant.now().minus(java.time.Duration.ofDays(3))))
                        .toList();
                model.addAttribute("recentPurchases", recentPurchases);
            } catch (Exception ex) {
                log.warn("Error loading purchases: {}", ex.getMessage());
            }

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
     * Страница выбора аватара. Используется и при первом входе (онбординг),
     * и позже, если ребёнок захочет сменить аватар — поэтому редиректа
     * "уже выбран → на dashboard" здесь нет.
     */
    @GetMapping("/select-avatar")
    public String selectAvatarPage(Model model) {
        try {
            ChildDto childProfile = userServiceClient.getMyChildProfile();
            model.addAttribute("childProfile", childProfile);
            List<AvatarOptionDto> avatarOptions = userServiceClient.getAvatarOptions();
            model.addAttribute("avatarOptions", avatarOptions);
        } catch (Exception e) {
            log.error("Error loading avatar selection: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки");
        }
        return "child/select-avatar";
    }

    /**
     * Выбрать (или сменить) аватар.
     * При первом выборе — редирект на страницу выбора фона (онбординг).
     */
    @PostMapping("/select-avatar")
    public String selectAvatar(
            @RequestParam UUID avatarOptionId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            boolean firstSelection = !userServiceClient.getMyChildProfile().avatarSelected();
            ChildDto child = userServiceClient.selectAvatar(avatarOptionId);

            if (firstSelection) {
                try {
                    taskServiceClient.createIntroTask(child.id());
                    log.info("Вводное задание создано для ребёнка {}", child.id());
                } catch (Exception ex) {
                    log.warn("Не удалось создать вводное задание для ребёнка {}: {}", child.id(), ex.getMessage());
                }
                // После первого выбора аватара направляем на выбор фона
                return "redirect:/child/select-background";
            }
            redirectAttributes.addFlashAttribute("success", "Аватар обновлён!");
        } catch (Exception e) {
            log.error("Error selecting avatar: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractError(e, "Ошибка выбора аватара"));
            return "redirect:/child/select-avatar";
        }
        return "redirect:/child/dashboard";
    }

    /**
     * Страница выбора фона. Онбординг после первого аватара или самостоятельная смена (с уровня 3).
     */
    @GetMapping("/select-background")
    public String selectBackgroundPage(Model model) {
        try {
            ChildDto childProfile = userServiceClient.getMyChildProfile();
            model.addAttribute("childProfile", childProfile);

            boolean isFirstSelection = !childProfile.backgroundSelected();
            boolean canChange = isFirstSelection || childProfile.level() >= 3;
            model.addAttribute("canChange", canChange);

            if (canChange) {
                List<BackgroundOptionDto> backgroundOptions = userServiceClient.getBackgroundOptions();
                model.addAttribute("backgroundOptions", backgroundOptions);
            }
        } catch (Exception e) {
            log.error("Error loading background selection: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки");
        }
        return "child/select-background";
    }

    /**
     * Выбрать (или сменить) фон.
     * При первом выборе — редирект на выбор акцентного цвета (онбординг).
     */
    @PostMapping("/select-background")
    public String selectBackground(
            @RequestParam UUID backgroundOptionId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            boolean firstSelection = !userServiceClient.getMyChildProfile().backgroundSelected();
            userServiceClient.selectBackground(backgroundOptionId);
            if (firstSelection) {
                return "redirect:/child/select-accent-color";
            }
            redirectAttributes.addFlashAttribute("success", "Фон выбран!");
        } catch (Exception e) {
            log.error("Error selecting background: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractError(e, "Ошибка выбора фона"));
            return "redirect:/child/select-background";
        }
        return "redirect:/child/dashboard";
    }

    /**
     * Страница выбора акцентного цвета. Онбординг после первого фона или самостоятельная смена.
     */
    @GetMapping("/select-accent-color")
    public String selectAccentColorPage(Model model) {
        try {
            ChildDto childProfile = userServiceClient.getMyChildProfile();
            model.addAttribute("childProfile", childProfile);
        } catch (Exception e) {
            log.error("Error loading accent color selection: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки");
        }
        return "child/select-accent-color";
    }

    /**
     * Выбрать (или сменить) акцентный цвет.
     */
    @PostMapping("/select-accent-color")
    public String selectAccentColor(
            @RequestParam String accentColor,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.selectAccentColor(accentColor);
            redirectAttributes.addFlashAttribute("success", "Цвет оформления сохранён!");
        } catch (Exception e) {
            log.error("Error selecting accent color: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", extractError(e, "Ошибка выбора цвета"));
            return "redirect:/child/select-accent-color";
        }
        return "redirect:/child/dashboard";
    }

    private String extractError(Exception e, String defaultMsg) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("\"message\":\"")) {
            int start = msg.indexOf("\"message\":\"") + 11;
            int end = msg.indexOf("\"", start);
            if (end > start) return msg.substring(start, end);
        }
        return defaultMsg;
    }

    /**
     * Ребёнок меняет свой никнейм.
     */
    @PostMapping("/profile/nickname")
    public String updateNickname(
            @RequestParam String nickname,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userServiceClient.updateMyNickname(java.util.Map.of("nickname", nickname));
            redirectAttributes.addFlashAttribute("success", "Никнейм обновлён!");
        } catch (Exception e) {
            log.error("Error updating nickname: {}", e.getMessage());
            String msg = e.getMessage() != null && e.getMessage().contains("\"message\":\"")
                    ? e.getMessage().substring(e.getMessage().indexOf("\"message\":\"") + 11,
                        e.getMessage().indexOf("\"", e.getMessage().indexOf("\"message\":\"") + 11))
                    : "Ошибка обновления никнейма";
            redirectAttributes.addFlashAttribute("error", msg);
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
            ChildDto profile = userServiceClient.getMyChildProfile();
            model.addAttribute("childProfile", profile);

            List<ShopItemDto> availableItems = userServiceClient.getAvailableShopItems();
            List<ShopPurchaseDto> myPurchases = userServiceClient.getMyPurchases();

            // Collect item IDs with pending purchases
            Set<UUID> pendingItemIds = myPurchases.stream()
                    .filter(p -> "PENDING".equals(p.status()) && p.shopItem() != null)
                    .map(p -> p.shopItem().id())
                    .collect(java.util.stream.Collectors.toSet());

            model.addAttribute("items", availableItems);
            model.addAttribute("purchases", myPurchases);
            model.addAttribute("pendingItemIds", pendingItemIds);
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
            log.error("Error requesting purchase for item {}: {}", itemId, e.getMessage());
            String errorMsg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Ошибка запроса покупки";
            redirectAttributes.addFlashAttribute("error", errorMsg);
        }
        return "redirect:/child/shop";
    }
}
