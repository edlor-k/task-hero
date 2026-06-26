package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.aop.LogMethod;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.UnauthorizedException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.PurchaseStatus;
import ru.taskhero.userservice.dto.ShopItemCreateRequest;
import ru.taskhero.userservice.dto.ShopItemResponseDto;
import ru.taskhero.userservice.dto.ShopItemUpdateRequest;
import ru.taskhero.userservice.dto.ShopPurchaseResponseDto;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.ShopItem;
import ru.taskhero.userservice.entity.ShopPurchase;
import ru.taskhero.userservice.entity.AuditAction;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.ShopItemRepository;
import ru.taskhero.userservice.repository.ShopPurchaseRepository;
import ru.taskhero.userservice.service.AuditService;
import ru.taskhero.userservice.service.ShopService;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Реализация сервиса магазина.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopItemRepository shopItemRepository;
    private final ShopPurchaseRepository shopPurchaseRepository;
    private final ChildRepository childRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    @LogMethod("shop-create-item")
    public ShopItemResponseDto createItem(UUID parentId, ShopItemCreateRequest request) {
        log.info("Создание товара '{}' родителем: {}", request.title(), parentId);

        if (request.childIds() == null || request.childIds().isEmpty()) {
            throw new ValidationException("Нужно указать хотя бы одного ребёнка");
        }
        Set<Child> children = new HashSet<>(childRepository.findAllById(request.childIds()));
        if (children.isEmpty()) {
            throw new ValidationException("Не найдены указанные дети");
        }

        ShopItem item = ShopItem.builder()
                .parentId(parentId)
                .title(request.title())
                .description(request.description())
                .priceCoins(request.priceCoins())
                .iconName(request.iconName() != null ? request.iconName() : "bi-gift")
                .active(true)
                .children(children)
                .build();

        item = shopItemRepository.save(item);
        log.info("Товар создан с ID: {}", item.getId());

        return toItemDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("shop-get-items-by-parent")
    public List<ShopItemResponseDto> getItemsByParent(UUID parentId) {
        return shopItemRepository.findAllByParentIdWithChildren(parentId).stream()
                .map(this::toItemDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("shop-get-items-for-child")
    public List<ShopItemResponseDto> getItemsForChild(UUID childId) {
        return shopItemRepository.findActiveByChildIdWithChildren(childId).stream()
                .map(this::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    @LogMethod("shop-delete-item")
    public void deleteItem(UUID itemId, UUID parentId) {
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));
        if (!item.getParentId().equals(parentId)) {
            throw new UnauthorizedException("Вы не можете управлять этим товаром");
        }
        item.setActive(false);
        shopItemRepository.save(item);
        log.info("Товар {} деактивирован (архив)", itemId);
    }

    @Override
    @Transactional
    @LogMethod("shop-update-item")
    public ShopItemResponseDto updateItem(UUID itemId, UUID parentId, ShopItemUpdateRequest request) {
        log.info("Обновление товара {} родителем {}", itemId, parentId);

        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));
        if (!item.getParentId().equals(parentId)) {
            throw new UnauthorizedException("Вы не можете управлять этим товаром");
        }

        if (request.title() != null) {
            item.setTitle(request.title());
        }
        if (request.description() != null) {
            item.setDescription(request.description());
        }
        if (request.priceCoins() != null) {
            item.setPriceCoins(request.priceCoins());
        }
        if (request.iconName() != null) {
            item.setIconName(request.iconName());
        }
        if (request.childIds() != null && !request.childIds().isEmpty()) {
            Set<Child> children = new HashSet<>(childRepository.findAllById(request.childIds()));
            if (children.isEmpty()) {
                throw new ValidationException("Не найдены указанные дети");
            }
            item.setChildren(children);
        }

        item = shopItemRepository.save(item);
        log.info("Товар {} обновлён", itemId);

        return toItemDto(item);
    }

    @Override
    @Transactional
    @LogMethod("shop-request-purchase")
    public ShopPurchaseResponseDto requestPurchase(UUID childId, UUID itemId) {
        log.info("Запрос покупки товара {} ребёнком {}", itemId, childId);

        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));

        if (!item.isActive()) {
            throw new ValidationException("Товар недоступен");
        }

        // Проверяем, что у ребёнка есть доступ к товару
        boolean hasAccess = item.getChildren().stream()
                .anyMatch(c -> c.getId().equals(childId));
        if (!hasAccess) {
            throw new UnauthorizedException("Этот товар вам недоступен");
        }

        // Проверяем, нет ли уже ожидающего запроса
        if (shopPurchaseRepository.existsByChildIdAndShopItemIdAndStatus(
                childId, itemId, PurchaseStatus.PENDING)) {
            throw new ValidationException("У вас уже есть запрос на этот товар");
        }

        // Проверяем, хватает ли коинов
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребёнок не найден"));
        if (child.getCoins() < item.getPriceCoins()) {
            throw new ValidationException("Недостаточно коинов. Нужно: "
                    + item.getPriceCoins() + ", у вас: " + child.getCoins());
        }

        ShopPurchase purchase = ShopPurchase.builder()
                .childId(childId)
                .shopItem(item)
                .status(PurchaseStatus.PENDING)
                .build();

        purchase = shopPurchaseRepository.save(purchase);
        log.info("Запрос на покупку создан: {}", purchase.getId());

        auditService.log(childId, null, AuditAction.SHOP_PURCHASE_REQUESTED,
                "PURCHASE", purchase.getId(), "Запрос на покупку \"" + item.getTitle() + "\"");

        return toPurchaseDto(purchase, child.getFirstName());
    }

    @Override
    @Transactional
    @LogMethod("shop-approve-purchase")
    public ShopPurchaseResponseDto approvePurchase(UUID purchaseId, UUID parentId, String comment) {
        log.info("Одобрение покупки {} родителем {}", purchaseId, parentId);

        ShopPurchase purchase = shopPurchaseRepository.findByIdWithItem(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Покупка не найдена"));

        if (!purchase.getShopItem().getParentId().equals(parentId)) {
            throw new UnauthorizedException("Вы не можете управлять этой покупкой");
        }

        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            throw new ValidationException("Покупка уже обработана");
        }

        Child child = childRepository.findById(purchase.getChildId())
                .orElseThrow(() -> new ResourceNotFoundException("Ребёнок не найден"));

        int price = purchase.getShopItem().getPriceCoins();
        if (child.getCoins() < price) {
            throw new ValidationException("У ребёнка недостаточно коинов ("
                    + child.getCoins() + " из " + price + ")");
        }

        // Списываем коины
        child.setCoins(child.getCoins() - price);
        childRepository.save(child);

        purchase.setStatus(PurchaseStatus.APPROVED);
        purchase.setParentComment(comment);
        purchase.setReviewedAt(Instant.now());
        purchase = shopPurchaseRepository.save(purchase);

        log.info("Покупка {} одобрена, списано {} коинов у ребёнка {}",
                purchaseId, price, purchase.getChildId());

        auditService.log(parentId, null, AuditAction.SHOP_PURCHASE_APPROVED,
                "PURCHASE", purchaseId, "Одобрена покупка \"" + purchase.getShopItem().getTitle() + "\"");

        return toPurchaseDto(purchase, child.getFirstName());
    }

    @Override
    @Transactional
    @LogMethod("shop-reject-purchase")
    public ShopPurchaseResponseDto rejectPurchase(UUID purchaseId, UUID parentId, String comment) {
        log.info("Отклонение покупки {} родителем {}", purchaseId, parentId);

        ShopPurchase purchase = shopPurchaseRepository.findByIdWithItem(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Покупка не найдена"));

        if (!purchase.getShopItem().getParentId().equals(parentId)) {
            throw new UnauthorizedException("Вы не можете управлять этой покупкой");
        }

        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            throw new ValidationException("Покупка уже обработана");
        }

        purchase.setStatus(PurchaseStatus.REJECTED);
        purchase.setParentComment(comment);
        purchase.setReviewedAt(Instant.now());
        purchase = shopPurchaseRepository.save(purchase);

        log.info("Покупка {} отклонена", purchaseId);

        auditService.log(parentId, null, AuditAction.SHOP_PURCHASE_REJECTED,
                "PURCHASE", purchaseId, "Отклонена покупка \"" + purchase.getShopItem().getTitle() + "\"");

        String childName = childRepository.findById(purchase.getChildId())
                .map(Child::getFirstName).orElse("Неизвестно");
        return toPurchaseDto(purchase, childName);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("shop-get-pending-purchases")
    public List<ShopPurchaseResponseDto> getPendingPurchases(UUID parentId) {
        List<ShopPurchase> purchases = shopPurchaseRepository.findAllByParentIdAndStatus(parentId, PurchaseStatus.PENDING);
        Map<UUID, String> childNames = resolveChildNames(purchases);
        return purchases.stream()
                .map(p -> toPurchaseDto(p, childNames.getOrDefault(p.getChildId(), "Неизвестно")))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("shop-get-purchases-by-child")
    public List<ShopPurchaseResponseDto> getPurchasesByChild(UUID childId) {
        String childName = childRepository.findById(childId)
                .map(Child::getFirstName).orElse("Неизвестно");
        return shopPurchaseRepository.findAllByChildId(childId).stream()
                .map(p -> toPurchaseDto(p, childName))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("shop-get-all-purchases-by-parent")
    public List<ShopPurchaseResponseDto> getAllPurchasesByParent(UUID parentId) {
        List<ShopPurchase> purchases = shopPurchaseRepository.findAllByParentId(parentId);
        Map<UUID, String> childNames = resolveChildNames(purchases);
        return purchases.stream()
                .map(p -> toPurchaseDto(p, childNames.getOrDefault(p.getChildId(), "Неизвестно")))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("shop-get-marketplace-items")
    public List<ShopItemResponseDto> getMarketplaceItems() {
        return shopItemRepository.findAllMarketplaceWithChildren().stream()
                .map(this::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    @LogMethod("shop-copy-from-marketplace")
    public ShopItemResponseDto copyFromMarketplace(UUID itemId, UUID parentId, List<UUID> childIds) {
        log.info("Копирование товара {} из маркетплейса для родителя {}", itemId, parentId);

        ShopItem source = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар маркетплейса не найден"));
        if (!source.isMarketplaceItem()) {
            throw new ValidationException("Указанный товар не является элементом маркетплейса");
        }

        Set<Child> children = new HashSet<>(childRepository.findAllById(childIds));
        if (children.isEmpty()) {
            throw new ValidationException("Не найдены указанные дети");
        }

        ShopItem copy = ShopItem.builder()
                .parentId(parentId)
                .title(source.getTitle())
                .description(source.getDescription())
                .priceCoins(source.getPriceCoins())
                .iconName(source.getIconName())
                .active(true)
                .marketplaceItem(false)
                .children(children)
                .build();

        copy = shopItemRepository.save(copy);
        log.info("Товар скопирован из маркетплейса: {}", copy.getId());
        return toItemDto(copy);
    }

    @Override
    @Transactional
    @LogMethod("shop-apply-preset")
    public int applyPreset(UUID parentId, UUID childId, String presetGroup) {
        log.info("Применение пресета '{}' родителем {} для ребёнка {}", presetGroup, parentId, childId);

        List<ShopItem> presetItems = shopItemRepository
                .findAllByMarketplaceItemTrueAndActiveAndPresetGroup(true, presetGroup);
        if (presetItems.isEmpty()) {
            throw new ValidationException("Пресет '" + presetGroup + "' не найден или пуст");
        }

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребёнок не найден"));

        Set<Child> children = Set.of(child);
        int count = 0;
        for (ShopItem source : presetItems) {
            ShopItem copy = ShopItem.builder()
                    .parentId(parentId)
                    .title(source.getTitle())
                    .description(source.getDescription())
                    .priceCoins(source.getPriceCoins())
                    .iconName(source.getIconName())
                    .active(true)
                    .marketplaceItem(false)
                    .presetGroup(source.getPresetGroup())
                    .children(new HashSet<>(children))
                    .build();
            shopItemRepository.save(copy);
            count++;
        }
        log.info("Пресет '{}' применён: {} товаров добавлено", presetGroup, count);
        return count;
    }

    /**
     * Конвертировать ShopItem в DTO.
     */
    private ShopItemResponseDto toItemDto(ShopItem item) {
        List<UUID> childIds = item.getChildren().stream()
                .map(Child::getId)
                .toList();
        return new ShopItemResponseDto(
                item.getId(),
                item.getParentId(),
                item.getTitle(),
                item.getDescription(),
                item.getPriceCoins(),
                item.getIconName(),
                item.isActive(),
                childIds,
                item.getCreatedAt(),
                item.isMarketplaceItem(),
                item.getPresetGroup()
        );
    }

    /**
     * Пакетная загрузка имён детей для списка покупок.
     */
    private Map<UUID, String> resolveChildNames(List<ShopPurchase> purchases) {
        List<UUID> childIds = purchases.stream()
                .map(ShopPurchase::getChildId)
                .distinct()
                .toList();
        return childRepository.findAllById(childIds).stream()
                .collect(Collectors.toMap(Child::getId, Child::getFirstName));
    }

    /**
     * Конвертировать ShopPurchase в DTO.
     */
    private ShopPurchaseResponseDto toPurchaseDto(ShopPurchase purchase, String childFirstName) {
        return new ShopPurchaseResponseDto(
                purchase.getId(),
                purchase.getChildId(),
                childFirstName,
                toItemDto(purchase.getShopItem()),
                purchase.getStatus(),
                purchase.getParentComment(),
                purchase.getCreatedAt(),
                purchase.getReviewedAt()
        );
    }

    @Override
    @Transactional
    @LogMethod("shop-create-marketplace-item")
    public ShopItemResponseDto createMarketplaceItem(ShopItemCreateRequest request) {
        log.info("Создание товара маркетплейса: {}", request.title());

        ShopItem item = ShopItem.builder()
                .parentId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .title(request.title())
                .description(request.description())
                .priceCoins(request.priceCoins())
                .iconName(request.iconName() != null ? request.iconName() : "bi-gift")
                .active(true)
                .marketplaceItem(true)
                .children(new HashSet<>())
                .build();

        item = shopItemRepository.save(item);
        log.info("Товар маркетплейса создан: {}", item.getId());
        return toItemDto(item);
    }

    @Override
    @Transactional
    @LogMethod("shop-update-marketplace-item")
    public ShopItemResponseDto updateMarketplaceItem(UUID itemId, ShopItemUpdateRequest request) {
        log.info("Обновление товара маркетплейса: {}", itemId);

        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар маркетплейса не найден"));
        if (!item.isMarketplaceItem()) {
            throw new ValidationException("Указанный товар не является элементом маркетплейса");
        }

        if (request.title() != null) item.setTitle(request.title());
        if (request.description() != null) item.setDescription(request.description());
        if (request.priceCoins() != null) item.setPriceCoins(request.priceCoins());
        if (request.iconName() != null) item.setIconName(request.iconName());

        item = shopItemRepository.save(item);
        log.info("Товар маркетплейса обновлён: {}", item.getId());
        return toItemDto(item);
    }

    @Override
    @Transactional
    @LogMethod("shop-delete-marketplace-item")
    public void deleteMarketplaceItem(UUID itemId) {
        log.info("Удаление товара маркетплейса: {}", itemId);

        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар маркетплейса не найден"));
        if (!item.isMarketplaceItem()) {
            throw new ValidationException("Указанный товар не является элементом маркетплейса");
        }

        shopItemRepository.delete(item);
        log.info("Товар маркетплейса удалён: {}", itemId);
    }
}
