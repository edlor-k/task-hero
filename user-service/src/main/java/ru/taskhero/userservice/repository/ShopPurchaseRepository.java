package ru.taskhero.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.taskhero.common.model.enums.PurchaseStatus;
import ru.taskhero.userservice.entity.ShopPurchase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с покупками.
 */
@Repository
public interface ShopPurchaseRepository extends JpaRepository<ShopPurchase, UUID> {

    /**
     * Найти все покупки ребёнка.
     */
    List<ShopPurchase> findAllByChildId(UUID childId);

    /**
     * Найти покупки ребёнка с определённым статусом.
     */
    List<ShopPurchase> findAllByChildIdAndStatus(UUID childId, PurchaseStatus status);

    /**
     * Найти ожидающие покупки для товаров указанного родителя.
     */
    @Query("SELECT sp FROM ShopPurchase sp JOIN FETCH sp.shopItem si "
            + "WHERE si.parentId = :parentId AND sp.status = :status")
    List<ShopPurchase> findAllByParentIdAndStatus(
            @Param("parentId") UUID parentId,
            @Param("status") PurchaseStatus status
    );

    /**
     * Найти покупку по ID с загруженным товаром.
     */
    @Query("SELECT sp FROM ShopPurchase sp JOIN FETCH sp.shopItem WHERE sp.id = :id")
    Optional<ShopPurchase> findByIdWithItem(@Param("id") UUID id);

    /**
     * Проверить, есть ли у ребёнка ожидающая покупка на этот товар.
     */
    boolean existsByChildIdAndShopItemIdAndStatus(UUID childId, UUID shopItemId, PurchaseStatus status);

    /**
     * Найти все покупки для товаров указанного родителя (история).
     */
    @Query("SELECT sp FROM ShopPurchase sp JOIN FETCH sp.shopItem si "
            + "WHERE si.parentId = :parentId ORDER BY sp.createdAt DESC")
    List<ShopPurchase> findAllByParentId(@Param("parentId") UUID parentId);
}
