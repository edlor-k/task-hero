package ru.taskhero.common.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Траектория сложности (трек) для ребёнка.
 * Определяет множитель EXP, получаемого за выполнение заданий.
 * Родитель выбирает трек при регистрации ребёнка.
 * Ребёнок НЕ видит свой трек — только сложность конкретных заданий.
 *
 * <ul>
 *   <li>EASY (Легко) — ×3 EXP, быстрый прогресс</li>
 *   <li>NORMAL (Нормально) — ×2 EXP, стандартный прогресс</li>
 *   <li>HARD (Сложно) — ×1 EXP, медленный прогресс</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum DifficultyTrajectory {
    /** Легко — ×3 EXP за задания, быстрый прогресс. */
    EASY(3.0, "Легко"),
    /** Нормально — ×2 EXP за задания, стандартный прогресс. */
    NORMAL(2.0, "Нормально"),
    /** Сложно — ×1 EXP за задания, медленный прогресс. */
    HARD(1.0, "Сложно");

    /** Множитель EXP, получаемого за выполнение заданий. */
    private final double expMultiplier;

    /** Отображаемое название. */
    private final String displayName;
}
