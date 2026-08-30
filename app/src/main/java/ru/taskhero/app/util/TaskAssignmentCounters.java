package ru.taskhero.app.util;

import ru.taskhero.app.dto.TaskAssignmentDto;

import java.util.List;

/**
 * Единое правило подсчёта заданий по статусу для счётчиков в UI (сайдбар, дашборд,
 * плашки). Счётчики обязаны считаться по тому же критерию, что и списки заданий,
 * которые они подытоживают — иначе бейдж и список под ним расходятся (баг «На
 * проверке» на кабинете ребёнка считал CREATED+SUBMITTED вместо только SUBMITTED,
 * то есть показывал ещё не сданные ребёнком задания как ожидающие решения родителя).
 */
public final class TaskAssignmentCounters {

    private TaskAssignmentCounters() {
    }

    /**
     * Количество заданий с указанным статусом (сравнение по имени статуса,
     * как он приходит из task-service через {@link TaskAssignmentDto#status()}).
     */
    public static long countByStatus(List<TaskAssignmentDto> assignments, String status) {
        if (assignments == null || assignments.isEmpty()) {
            return 0;
        }
        return assignments.stream()
                .filter(a -> status.equals(a.status()))
                .count();
    }
}
