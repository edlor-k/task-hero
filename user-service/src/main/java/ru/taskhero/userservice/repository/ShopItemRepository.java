package ru.taskhero.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.taskhero.userservice.entity.ShopItem;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с товарами магазина.
 */
@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, UUID> {

    /**
     * Найти все товары родителя.
     */
    List<ShopItem> findAllByParentId(UUID parentId);

    /**
     * Найти активные товары родителя.
     */
    List<ShopItem> findAllByParentIdAndActive(UUID parentId, boolean active);

    /**
     * Найти активные товары, доступные конкретному ребёнку.
     */
    @Query("SELECT si FROM ShopItem si JOIN si.children c WHERE c.id = :childId AND si.active = true")
    List<ShopItem> findActiveByChildId(@Param("childId") UUID childId);

    /**
     * Найти все элементы маркетплейса.
     */
    List<ShopItem> findAllByMarketplaceItemTrue();

    /**
     * Найти все товары родителя с загруженными детьми.
     */
    @Query("SELECT DISTINCT si FROM ShopItem si LEFT JOIN FETCH si.children WHERE si.parentId = :parentId")
    List<ShopItem> findAllByParentIdWithChildren(@Param("parentId") UUID parentId);

    /**
     * Найти активные товары для ребёнка с загруженными детьми.
     */
    @Query("SELECT DISTINCT si FROM ShopItem si LEFT JOIN FETCH si.children WHERE si.active = true AND EXISTS (SELECT 1 FROM si.children c WHERE c.id = :childId)")
    List<ShopItem> findActiveByChildIdWithChildren(@Param("childId") UUID childId);

    /**
     * Найти все элементы маркетплейса с загруженными детьми.
     */
    @Query("SELECT DISTINCT si FROM ShopItem si LEFT JOIN FETCH si.children WHERE si.marketplaceItem = true")
    List<ShopItem> findAllMarketplaceWithChildren();
}
