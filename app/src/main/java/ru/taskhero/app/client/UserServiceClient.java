package ru.taskhero.app.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.taskhero.app.dto.ChildDto;
import ru.taskhero.app.dto.CreateChildRequest;
import ru.taskhero.app.dto.LevelInfoDto;
import ru.taskhero.app.dto.LevelRewardDto;
import ru.taskhero.app.dto.LoginRequest;
import ru.taskhero.app.dto.LoginResponse;
import ru.taskhero.app.dto.ShopItemDto;
import ru.taskhero.app.dto.ShopPurchaseDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign клиент для User Service.
 */
@FeignClient(
        name = "user-service",
        url = "${services.user-service.url}"
)
public interface UserServiceClient {

    /**
     * Вход пользователя.
     */
    @PostMapping("/auth/login")
    LoginResponse login(@RequestBody LoginRequest request);

    /**
     * Регистрация нового пользователя.
     */
    @PostMapping("/auth/register")
    Map<String, Object> register(@RequestBody Map<String, String> request);

    /**
     * Вход ребёнка по токену.
     */
    @PostMapping("/auth/login/child")
    LoginResponse loginChild(@RequestBody Map<String, String> request);

    /**
     * Получить список детей текущего родителя.
     */
    @GetMapping("/children")
    List<ChildDto> getChildren();

    /**
     * Получить профиль текущего ребёнка (по JWT).
     */
    @GetMapping("/children/me")
    ChildDto getMyChildProfile();

    /**
     * Выбрать персонажа (ребёнок, первый вход).
     */
    @PostMapping("/children/me/character")
    ChildDto selectCharacter(@RequestParam("characterType") String characterType);

    /**
     * Добавить ребёнка.
     */
    @PostMapping("/children")
    ChildDto createChild(@RequestBody CreateChildRequest request);

    /**
     * Получить ребёнка по ID.
     */
    @GetMapping("/children/{id}")
    ChildDto getChildById(@PathVariable("id") UUID id);

    // ==================== Shop ====================

    /**
     * Создать товар в магазине.
     */
    @PostMapping("/shop/items")
    ShopItemDto createShopItem(@RequestBody Map<String, Object> request);

    /**
     * Получить товары родителя.
     */
    @GetMapping("/shop/items")
    List<ShopItemDto> getShopItems();

    /**
     * Удалить товар.
     */
    @DeleteMapping("/shop/items/{id}")
    void deleteShopItem(@PathVariable("id") UUID id);

    /**
     * Обновить товар.
     */
    @PutMapping("/shop/items/{id}")
    ShopItemDto updateShopItem(@PathVariable("id") UUID id, @RequestBody Map<String, Object> request);

    /**
     * Получить ожидающие покупки.
     */
    @GetMapping("/shop/purchases/pending")
    List<ShopPurchaseDto> getPendingPurchases();

    /**
     * Одобрить покупку.
     */
    @PostMapping("/shop/purchases/{id}/approve")
    ShopPurchaseDto approvePurchase(
            @PathVariable("id") UUID id,
            @RequestParam(required = false) String comment
    );

    /**
     * Отклонить покупку.
     */
    @PostMapping("/shop/purchases/{id}/reject")
    ShopPurchaseDto rejectPurchase(
            @PathVariable("id") UUID id,
            @RequestParam(required = false) String comment
    );

    /**
     * Получить доступные товары для ребёнка.
     */
    @GetMapping("/shop/items/available")
    List<ShopItemDto> getAvailableShopItems();

    /**
     * Запросить покупку.
     */
    @PostMapping("/shop/purchases/request/{itemId}")
    ShopPurchaseDto requestPurchase(@PathVariable("itemId") UUID itemId);

    /**
     * Получить мои покупки (ребёнок).
     */
    @GetMapping("/shop/purchases/my")
    List<ShopPurchaseDto> getMyPurchases();

    /**
     * Получить всю историю покупок (родитель).
     */
    @GetMapping("/shop/purchases/history")
    List<ShopPurchaseDto> getAllPurchases();

    /**
     * Получить товары маркетплейса.
     */
    @GetMapping("/shop/marketplace")
    List<ShopItemDto> getMarketplaceItems();

    /**
     * Добавить товар из маркетплейса в свой магазин.
     */
    @PostMapping("/shop/marketplace/{id}/copy")
    ShopItemDto copyFromMarketplace(@PathVariable("id") UUID id, @RequestBody Map<String, Object> request);

    // ==================== Level Rewards ====================

    /**
     * Создать награды за уровни (bulk).
     */
    @PostMapping("/level-rewards/children/{childId}")
    List<LevelRewardDto> createLevelRewards(
            @PathVariable("childId") UUID childId,
            @RequestBody Map<String, Object> request
    );

    /**
     * Получить все награды ребёнка за уровни.
     */
    @GetMapping("/level-rewards/children/{childId}")
    List<LevelRewardDto> getLevelRewards(@PathVariable("childId") UUID childId);

    /**
     * Получить информацию об уровнях (XP, примерное кол-во заданий).
     */
    @GetMapping("/level-rewards/children/{childId}/level-info")
    List<LevelInfoDto> getLevelInfo(
            @PathVariable("childId") UUID childId,
            @RequestParam("from") int from,
            @RequestParam("to") int to
    );

    /**
     * Получить количество незаполненных уровней без наград.
     */
    @GetMapping("/level-rewards/children/{childId}/unfilled-count")
    Map<String, Integer> getUnfilledCount(@PathVariable("childId") UUID childId);

    /**
     * Отметить награду как выданную родителем.
     */
    @PostMapping("/level-rewards/{id}/issued")
    LevelRewardDto markRewardIssued(@PathVariable("id") UUID id);

    /**
     * Удалить награду за уровень.
     */
    @DeleteMapping("/level-rewards/{id}")
    void deleteLevelReward(@PathVariable("id") UUID id);

    /**
     * Получить непросмотренные награды ребёнка.
     */
    @GetMapping("/level-rewards/children/{childId}/unseen")
    List<LevelRewardDto> getUnseenRewards(@PathVariable("childId") UUID childId);

    /**
     * Пометить награду как просмотренную.
     */
    @PostMapping("/level-rewards/{id}/seen")
    void markRewardSeen(@PathVariable("id") UUID id);
}
