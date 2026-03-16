package ru.taskhero.common.model.enums;

/**
 * Статус покупки в магазине.
 */
public enum PurchaseStatus {
    /** Ожидает одобрения родителя. */
    PENDING,
    /** Покупка одобрена, коины списаны. */
    APPROVED,
    /** Покупка отклонена родителем. */
    REJECTED
}
