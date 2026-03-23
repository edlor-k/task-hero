package ru.taskhero.userservice.service;

import ru.taskhero.userservice.dto.ShopItemCreateRequest;
import ru.taskhero.userservice.dto.ShopItemResponseDto;
import ru.taskhero.userservice.dto.ShopItemUpdateRequest;
import ru.taskhero.userservice.dto.ShopPurchaseResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления магазином.
 */
public interface ShopService {

    /**
     * Создать товар в магазине.
     */
    ShopItemResponseDto createItem(UUID parentId, ShopItemCreateRequest request);

    /**
     * Получить все товары родителя.
     */
    List<ShopItemResponseDto> getItemsByParent(UUID parentId);

    /**
     * Получить товары, доступные ребёнку.
     */
    List<ShopItemResponseDto> getItemsForChild(UUID childId);

    /**
     * Удалить товар.
     */
    void deleteItem(UUID itemId, UUID parentId);

    /**
     * Обновить товар.
     */
    ShopItemResponseDto updateItem(UUID itemId, UUID parentId, ShopItemUpdateRequest request);

    /**
     * Ребёнок запрашивает покупку товара.
     */
    ShopPurchaseResponseDto requestPurchase(UUID childId, UUID itemId);

    /**
     * Родитель одобряет покупку (коины списываются).
     */
    ShopPurchaseResponseDto approvePurchase(UUID purchaseId, UUID parentId, String comment);

    /**
     * Родитель отклоняет покупку.
     */
    ShopPurchaseResponseDto rejectPurchase(UUID purchaseId, UUID parentId, String comment);

    /**
     * Получить ожидающие покупки для родителя.
     */
    List<ShopPurchaseResponseDto> getPendingPurchases(UUID parentId);

    /**
     * Получить покупки ребёнка.
     */
    List<ShopPurchaseResponseDto> getPurchasesByChild(UUID childId);

    /**
     * Получить всю историю покупок для родителя (все статусы).
     */
    List<ShopPurchaseResponseDto> getAllPurchasesByParent(UUID parentId);

    /**
     * Получить элементы глобального маркетплейса (системные шаблоны наград).
     */
    List<ShopItemResponseDto> getMarketplaceItems();

    /**
     * Скопировать товар из маркетплейса в магазин родителя.
     */
    ShopItemResponseDto copyFromMarketplace(UUID itemId, UUID parentId, List<UUID> childIds);

    /**
     * Создать системный товар в маркетплейсе (для админа).
     */
    ShopItemResponseDto createMarketplaceItem(ShopItemCreateRequest request);

    /**
     * Обновить системный товар маркетплейса (для админа).
     */
    ShopItemResponseDto updateMarketplaceItem(UUID itemId, ShopItemUpdateRequest request);

    /**
     * Удалить системный товар маркетплейса (для админа).
     */
    void deleteMarketplaceItem(UUID itemId);
}
