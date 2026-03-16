package ru.taskhero.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Сущность награды за достижение уровня.
 * Родитель создаёт награды, которые автоматически начисляются ребёнку при достижении уровня.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "level_rewards",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_level_rewards_child_level",
                columnNames = {"child_id", "level"}))
public class LevelReward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "shop_item_id")
    private UUID shopItemId;

    @Builder.Default
    @Column(name = "is_claimed", nullable = false)
    private boolean claimed = false;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Builder.Default
    @Column(name = "is_issued", nullable = false)
    private boolean issued = false;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Builder.Default
    @Column(name = "is_seen", nullable = false)
    private boolean seen = false;
}
