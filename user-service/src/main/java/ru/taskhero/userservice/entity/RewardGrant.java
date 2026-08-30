package ru.taskhero.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;

import java.util.UUID;

/**
 * Леджер применённых начислений наград, привязанных к назначению задания
 * (или другой опознаваемой операции) в task-service.
 * <p>
 * Уникальное ограничение на {@code sourceAssignmentId} — ключ идемпотентности:
 * при повторном запросе на начисление за то же назначение (ретрай после сетевого
 * сбоя, двойной клик, конкурентные запросы) вставка новой строки нарушит
 * ограничение БД, и повторное начисление баланса ребёнку не произойдёт.
 * NULL в {@code sourceAssignmentId} не участвует в уникальном ограничении
 * (для начислений без источника, например ручных операций администратора).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "reward_grants")
public class RewardGrant extends BaseEntity {

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "source_assignment_id", unique = true)
    private UUID sourceAssignmentId;

    @Column(name = "exp", nullable = false)
    private int exp;

    @Column(name = "coins", nullable = false)
    private int coins;
}
