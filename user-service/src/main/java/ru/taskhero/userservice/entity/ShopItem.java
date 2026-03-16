package ru.taskhero.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Сущность товара в магазине.
 * Родители создают товары, которые дети могут приобретать за коины.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "shop_items")
public class ShopItem extends BaseEntity {

    /**
     * ID родителя, создавшего товар.
     */
    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    /**
     * Название товара.
     */
    @Column(name = "title", nullable = false, length = 128)
    private String title;

    /**
     * Описание товара.
     */
    @Column(name = "description", length = 512)
    private String description;

    /**
     * Цена в коинах.
     */
    @Column(name = "price_coins", nullable = false)
    private int priceCoins;

    /**
     * Название иконки Bootstrap Icons (например: bi-gift, bi-controller).
     */
    @Builder.Default
    @Column(name = "icon_name", nullable = false, length = 64)
    private String iconName = "bi-gift";

    /**
     * Активен ли товар.
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * Является ли товар элементом глобального маркетплейса (системный шаблон).
     */
    @Builder.Default
    @Column(name = "is_marketplace", nullable = false)
    private boolean marketplaceItem = false;

    /**
     * Дети, для которых доступен этот товар.
     */
    @ManyToMany
    @JoinTable(
            name = "shop_item_children",
            joinColumns = @JoinColumn(name = "shop_item_id"),
            inverseJoinColumns = @JoinColumn(name = "child_id")
    )
    @Builder.Default
    private Set<Child> children = new HashSet<>();
}
