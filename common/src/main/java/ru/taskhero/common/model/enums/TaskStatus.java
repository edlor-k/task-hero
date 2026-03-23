package ru.taskhero.common.model.enums;

/**
 * Перечисление возможных статусов задачи.
 */
public enum TaskStatus {
    /** Статус созданной задачи. */
    CREATED,
    /** Статус отправленной задачи. */
    SUBMITTED,
    /** Статус одобренной задачи. */
    APPROVED,
    /** Статус отклоненной задачи. */
    REJECTED,
    /** Статус просроченной задачи (дедлайн истёк). */
    EXPIRED,
}
