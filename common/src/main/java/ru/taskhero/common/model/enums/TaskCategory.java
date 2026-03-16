package ru.taskhero.common.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Категория задания для библиотеки заданий.
 */
@Getter
@RequiredArgsConstructor
public enum TaskCategory {
    STUDY("Учёба", "bi-book"),
    HOUSEHOLD("По дому", "bi-house"),
    SPORTS("Спорт", "bi-trophy"),
    CREATIVITY("Творчество", "bi-palette"),
    HYGIENE("Гигиена", "bi-droplet"),
    SOCIAL("Общение", "bi-people"),
    READING("Чтение", "bi-journal-text"),
    OTHER("Другое", "bi-three-dots");

    /** Отображаемое название. */
    private final String displayName;

    /** Bootstrap Icons класс. */
    private final String iconClass;
}
