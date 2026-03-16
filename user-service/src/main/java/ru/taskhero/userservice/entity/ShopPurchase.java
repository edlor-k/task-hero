package ru.taskhero.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;
import ru.taskhero.common.model.enums.PurchaseStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Сущность покупки в магазине.
 * Ребёнок отправляет запрос на покупку, родитель одобряет или отклоняет.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "shop_purchases")
public class ShopPurchase extends BaseEntity {

    /**
     * ID ребёнка-покупателя.
     */
    @Column(name = "child_id", nullable = false)
    private UUID childId;

    /**
     * Товар, который приобретается.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private ShopItem shopItem;

    /**
     * Статус покупки.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PurchaseStatus status = PurchaseStatus.PENDING;

    /**
     * Комментарий родителя при проверке.
     */
    @Column(name = "parent_comment", length = 512)
    private String parentComment;

    /**
     * Время проверки покупки родителем.
     */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
