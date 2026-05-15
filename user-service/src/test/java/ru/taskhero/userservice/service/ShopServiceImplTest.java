package ru.taskhero.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.UnauthorizedException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.PurchaseStatus;
import ru.taskhero.userservice.dto.ShopItemCreateRequest;
import ru.taskhero.userservice.dto.ShopItemResponseDto;
import ru.taskhero.userservice.dto.ShopPurchaseResponseDto;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.ShopItem;
import ru.taskhero.userservice.entity.ShopPurchase;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.ShopItemRepository;
import ru.taskhero.userservice.repository.ShopPurchaseRepository;
import ru.taskhero.userservice.service.impl.ShopServiceImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService Unit Tests")
class ShopServiceImplTest {

    @Mock
    private ShopItemRepository shopItemRepository;

    @Mock
    private ShopPurchaseRepository shopPurchaseRepository;

    @Mock
    private ChildRepository childRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ShopServiceImpl shopService;

    private UUID parentId;
    private UUID childId;
    private UUID itemId;
    private UUID purchaseId;
    private Child child;
    private ShopItem shopItem;
    private ShopPurchase purchase;

    @BeforeEach
    void setUp() {
        parentId = UUID.randomUUID();
        childId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        purchaseId = UUID.randomUUID();

        child = Child.builder()
                .firstName("Алиса")
                .coins(100)
                .build();

        Set<Child> children = new HashSet<>();
        children.add(child);

        shopItem = ShopItem.builder()
                .parentId(parentId)
                .title("Поход в кино")
                .description("Билет на фильм")
                .priceCoins(50)
                .iconName("bi-film")
                .active(true)
                .children(children)
                .build();

        purchase = ShopPurchase.builder()
                .childId(childId)
                .shopItem(shopItem)
                .status(PurchaseStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Должен создать товар в магазине")
    void shouldCreateShopItem() {
        // Given
        ShopItemCreateRequest request = new ShopItemCreateRequest(
                "Поход в кино", "Билет на фильм", 50, "bi-film", List.of(childId)
        );
        when(childRepository.findAllById(List.of(childId))).thenReturn(List.of(child));
        when(shopItemRepository.save(any(ShopItem.class))).thenReturn(shopItem);

        // When
        ShopItemResponseDto result = shopService.createItem(parentId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Поход в кино");
        assertThat(result.priceCoins()).isEqualTo(50);
        verify(shopItemRepository).save(any(ShopItem.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при создании товара без детей")
    void shouldThrowExceptionWhenNoChildrenFound() {
        // Given
        ShopItemCreateRequest request = new ShopItemCreateRequest(
                "Товар", null, 10, null, List.of(UUID.randomUUID())
        );
        when(childRepository.findAllById(any())).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> shopService.createItem(parentId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Не найдены указанные дети");
    }

    @Test
    @DisplayName("Должен создать запрос на покупку")
    void shouldRequestPurchase() {
        // Given
        // Сохраним childId в child для проверки доступа
        java.lang.reflect.Field idField;
        try {
            idField = ru.taskhero.common.model.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(child, childId);
        } catch (Exception ignored) {}

        when(shopItemRepository.findById(itemId)).thenReturn(Optional.of(shopItem));
        when(shopPurchaseRepository.existsByChildIdAndShopItemIdAndStatus(childId, itemId, PurchaseStatus.PENDING))
                .thenReturn(false);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(shopPurchaseRepository.save(any(ShopPurchase.class))).thenReturn(purchase);

        // When
        ShopPurchaseResponseDto result = shopService.requestPurchase(childId, itemId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(PurchaseStatus.PENDING);
        verify(shopPurchaseRepository).save(any(ShopPurchase.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при запросе покупки несуществующего товара")
    void shouldThrowExceptionForUnknownItem() {
        // Given
        when(shopItemRepository.findById(itemId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shopService.requestPurchase(childId, itemId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Товар не найден");
    }

    @Test
    @DisplayName("Должен выбросить исключение при недостатке коинов у ребёнка")
    void shouldThrowExceptionWhenNotEnoughCoins() {
        // Given
        child.setCoins(10);
        try {
            java.lang.reflect.Field idField = ru.taskhero.common.model.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(child, childId);
        } catch (Exception ignored) {}

        when(shopItemRepository.findById(itemId)).thenReturn(Optional.of(shopItem));
        when(shopPurchaseRepository.existsByChildIdAndShopItemIdAndStatus(childId, itemId, PurchaseStatus.PENDING))
                .thenReturn(false);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        // When & Then
        assertThatThrownBy(() -> shopService.requestPurchase(childId, itemId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Недостаточно коинов");
    }

    @Test
    @DisplayName("Должен одобрить покупку и списать коины")
    void shouldApprovePurchaseAndDeductCoins() {
        // Given
        child.setCoins(100);
        when(shopPurchaseRepository.findByIdWithItem(purchaseId)).thenReturn(Optional.of(purchase));
        when(childRepository.findById(purchase.getChildId())).thenReturn(Optional.of(child));
        when(childRepository.save(any(Child.class))).thenReturn(child);
        when(shopPurchaseRepository.save(any(ShopPurchase.class))).thenReturn(purchase);

        // When
        ShopPurchaseResponseDto result = shopService.approvePurchase(purchaseId, parentId, "Молодец!");

        // Then
        assertThat(result).isNotNull();
        verify(childRepository).save(any(Child.class));
        verify(shopPurchaseRepository).save(any(ShopPurchase.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при одобрении чужой покупки")
    void shouldThrowExceptionWhenApprovingOthersPurchase() {
        // Given
        UUID otherParentId = UUID.randomUUID();
        when(shopPurchaseRepository.findByIdWithItem(purchaseId)).thenReturn(Optional.of(purchase));

        // When & Then
        assertThatThrownBy(() -> shopService.approvePurchase(purchaseId, otherParentId, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("не можете управлять");
    }

    @Test
    @DisplayName("Должен отклонить покупку")
    void shouldRejectPurchase() {
        // Given
        when(shopPurchaseRepository.findByIdWithItem(purchaseId)).thenReturn(Optional.of(purchase));
        when(shopPurchaseRepository.save(any(ShopPurchase.class))).thenReturn(purchase);
        when(childRepository.findById(purchase.getChildId())).thenReturn(Optional.of(child));

        // When
        ShopPurchaseResponseDto result = shopService.rejectPurchase(purchaseId, parentId, "Нет пока");

        // Then
        assertThat(result).isNotNull();
        verify(shopPurchaseRepository).save(any(ShopPurchase.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при повторной обработке покупки")
    void shouldThrowExceptionWhenPurchaseAlreadyProcessed() {
        // Given
        purchase.setStatus(PurchaseStatus.APPROVED);
        when(shopPurchaseRepository.findByIdWithItem(purchaseId)).thenReturn(Optional.of(purchase));

        // When & Then
        assertThatThrownBy(() -> shopService.approvePurchase(purchaseId, parentId, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("уже обработана");
    }

    @Test
    @DisplayName("Должен удалить товар из магазина")
    void shouldDeleteShopItem() {
        // Given
        when(shopItemRepository.findById(itemId)).thenReturn(Optional.of(shopItem));

        // When
        shopService.deleteItem(itemId, parentId);

        // Then
        verify(shopItemRepository).delete(shopItem);
    }

    @Test
    @DisplayName("Должен выбросить исключение при удалении чужого товара")
    void shouldThrowExceptionWhenDeletingOthersItem() {
        // Given
        UUID otherParentId = UUID.randomUUID();
        when(shopItemRepository.findById(itemId)).thenReturn(Optional.of(shopItem));

        // When & Then
        assertThatThrownBy(() -> shopService.deleteItem(itemId, otherParentId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("не можете управлять");
    }
}
