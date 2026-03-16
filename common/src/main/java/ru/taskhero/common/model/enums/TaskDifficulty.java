package ru.taskhero.common.model.enums;

/**
 * Уровень сложности задания.
 * Влияет на множитель EXP-награды.
 */
public enum TaskDifficulty {
    /** Лёгкое задание (×0.8 EXP). */
    EASY,
    /** Обычное задание (×1.0 EXP). */
    NORMAL,
    /** Сложное задание (×1.5 EXP). */
    HARD
}
