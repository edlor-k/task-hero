package ru.taskhero.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.taskhero.userservice.dto.ShopItemCreateRequest;
import ru.taskhero.userservice.dto.ShopItemResponseDto;
import ru.taskhero.userservice.dto.ShopItemUpdateRequest;
import ru.taskhero.userservice.dto.ShopPurchaseResponseDto;
import ru.taskhero.userservice.service.ParentService;
import ru.taskhero.userservice.service.ShopService;
import ru.taskhero.userservice.util.SecurityUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер для магазина товаров.
 */
@Slf4j
@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
@Tag(name = "Shop", description = "Магазин товаров за коины")
@SecurityRequirement(name = "Bearer Authentication")
public class ShopController {

    private final ShopService shopService;
    private final ParentService parentService;

    // ==================== PARENT ENDPOINTS ====================

    /**
     * Создать товар в магазине.
     */
    @PostMapping("/items")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Создать товар", description = "Родитель создаёт товар в магазине")
    public ResponseEntity<ShopItemResponseDto> createItem(@Valid @RequestBody ShopItemCreateRequest request) {
        UUID parentId = getParentId();
        ShopItemResponseDto response = shopService.createItem(parentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получить все товары родителя.
     */
    @GetMapping("/items")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Товары родителя", description = "Получить все товары текущего родителя")
    public ResponseEntity<List<ShopItemResponseDto>> getMyItems() {
        UUID parentId = getParentId();
        return ResponseEntity.ok(shopService.getItemsByParent(parentId));
    }

    /**
     * Удалить товар.
     */
    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Удалить товар")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
        UUID parentId = getParentId();
        shopService.deleteItem(id, parentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Обновить товар.
     */
    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Обновить товар", description = "Родитель обновляет товар в магазине")
    public ResponseEntity<ShopItemResponseDto> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody ShopItemUpdateRequest request
    ) {
        UUID parentId = getParentId();
        ShopItemResponseDto response = shopService.updateItem(id, parentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить ожидающие покупки для родителя.
     */
    @GetMapping("/purchases/pending")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Ожидающие покупки", description = "Получить покупки, ожидающие одобрения")
    public ResponseEntity<List<ShopPurchaseResponseDto>> getPendingPurchases() {
        UUID parentId = getParentId();
        return ResponseEntity.ok(shopService.getPendingPurchases(parentId));
    }

    /**
     * Одобрить покупку.
     */
    @PostMapping("/purchases/{id}/approve")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Одобрить покупку")
    public ResponseEntity<ShopPurchaseResponseDto> approvePurchase(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment
    ) {
        UUID parentId = getParentId();
        return ResponseEntity.ok(shopService.approvePurchase(id, parentId, comment));
    }

    /**
     * Отклонить покупку.
     */
    @PostMapping("/purchases/{id}/reject")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Отклонить покупку")
    public ResponseEntity<ShopPurchaseResponseDto> rejectPurchase(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment
    ) {
        UUID parentId = getParentId();
        return ResponseEntity.ok(shopService.rejectPurchase(id, parentId, comment));
    }

    // ==================== CHILD ENDPOINTS ====================

    /**
     * Получить доступные товары для текущего ребёнка.
     */
    @GetMapping("/items/available")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Доступные товары", description = "Получить товары, доступные ребёнку")
    public ResponseEntity<List<ShopItemResponseDto>> getAvailableItems() {
        UUID childId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(shopService.getItemsForChild(childId));
    }

    /**
     * Запросить покупку товара.
     */
    @PostMapping("/purchases/request/{itemId}")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Запросить покупку", description = "Ребёнок запрашивает покупку товара")
    public ResponseEntity<ShopPurchaseResponseDto> requestPurchase(@PathVariable UUID itemId) {
        UUID childId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                shopService.requestPurchase(childId, itemId));
    }

    /**
     * Получить мои покупки (для ребёнка).
     */
    @GetMapping("/purchases/my")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Мои покупки", description = "Получить историю покупок ребёнка")
    public ResponseEntity<List<ShopPurchaseResponseDto>> getMyPurchases() {
        UUID childId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(shopService.getPurchasesByChild(childId));
    }

    /**
     * Получить ID родителя текущего пользователя.
     */
    private UUID getParentId() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return parentService.getByUserId(userId).id();
    }

    // ==================== HISTORY & MARKETPLACE ENDPOINTS ====================

    /**
     * Получить всю историю покупок для родителя (все статусы).
     */
    @GetMapping("/purchases/history")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "История всех покупок", description = "Получить полную историю покупок для товаров родителя")
    public ResponseEntity<List<ShopPurchaseResponseDto>> getAllPurchases() {
        UUID parentId = getParentId();
        return ResponseEntity.ok(shopService.getAllPurchasesByParent(parentId));
    }

    /**
     * Получить список товаров глобального маркетплейса.
     */
    @GetMapping("/marketplace")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Маркетплейс наград", description = "Список готовых наград из глобального каталога")
    public ResponseEntity<List<ShopItemResponseDto>> getMarketplace() {
        return ResponseEntity.ok(shopService.getMarketplaceItems());
    }

    /**
     * Скопировать товар из маркетплейса в собственный магазин.
     */
    @PostMapping("/marketplace/{id}/copy")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Добавить из маркетплейса",
            description = "Скопировать готовую награду из маркетплейса в свой магазин")
    public ResponseEntity<ShopItemResponseDto> copyFromMarketplace(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request
    ) {
        UUID parentId = getParentId();
        @SuppressWarnings("unchecked")
        List<String> childIdStrings = (List<String>) request.get("childIds");
        List<UUID> childIds = childIdStrings.stream().map(UUID::fromString).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                shopService.copyFromMarketplace(id, parentId, childIds));
    }

    /**
     * Применить пресет наград: массово скопировать все товары группы в магазин родителя.
     */
    @PostMapping("/marketplace/preset/apply")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Применить пресет наград",
            description = "Массово добавить все товары группы маркетплейса в свой магазин")
    public ResponseEntity<Map<String, Object>> applyPreset(
            @RequestParam String presetGroup,
            @RequestParam UUID childId
    ) {
        UUID parentId = getParentId();
        int count = shopService.applyPreset(parentId, childId, presetGroup);
        return ResponseEntity.ok(Map.of("added", count, "presetGroup", presetGroup));
    }
}
