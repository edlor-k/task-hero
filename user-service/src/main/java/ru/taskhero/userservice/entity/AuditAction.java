package ru.taskhero.userservice.entity;

/**
 * Коды действий для журнала аудита.
 */
public enum AuditAction {
    USER_CREATED,
    USER_BLOCKED,
    USER_UNBLOCKED,
    USER_ROLE_CHANGED,
    USER_DELETED,
    PARENT_UPDATED,
    CHILD_UPDATED,
    CHILD_TOKEN_RESET,
    CHILD_EXP_ADJUSTED,
    CHILD_COINS_ADJUSTED,
    CHILD_LEVEL_ADJUSTED
}
