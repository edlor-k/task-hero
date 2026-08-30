package ru.taskhero.app.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.taskhero.app.dto.TaskAssignmentDto;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регрессионный тест на баг «На проверке» на кабинете ребёнка: счётчик считал
 * CREATED+SUBMITTED (getMyActiveAssignments) вместо только SUBMITTED, из-за чего
 * показывал ещё не сданные ребёнком задания как ожидающие решения родителя —
 * расхождение с тем, как этот же статус трактует родительский кабинет.
 */
@DisplayName("TaskAssignmentCounters")
class TaskAssignmentCountersTest {

    private TaskAssignmentDto dto(String status) {
        return new TaskAssignmentDto(
                UUID.randomUUID(), UUID.randomUUID(), status, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null
        );
    }

    @Test
    @DisplayName("Должен считать только задания с точным статусом, не все «активные» (CREATED+SUBMITTED)")
    void shouldCountOnlyExactStatus() {
        List<TaskAssignmentDto> assignments = List.of(
                dto("CREATED"), dto("CREATED"), dto("SUBMITTED"), dto("APPROVED"), dto("REJECTED")
        );

        assertThat(TaskAssignmentCounters.countByStatus(assignments, "SUBMITTED")).isEqualTo(1);
        assertThat(TaskAssignmentCounters.countByStatus(assignments, "CREATED")).isEqualTo(2);
        assertThat(TaskAssignmentCounters.countByStatus(assignments, "APPROVED")).isEqualTo(1);
    }

    @Test
    @DisplayName("Должен вернуть 0 для пустого или null списка")
    void shouldReturnZeroForEmptyOrNullList() {
        assertThat(TaskAssignmentCounters.countByStatus(List.of(), "SUBMITTED")).isZero();
        assertThat(TaskAssignmentCounters.countByStatus(null, "SUBMITTED")).isZero();
    }

    @Test
    @DisplayName("«Активные» список (CREATED+SUBMITTED) не должен считаться как «На проверке» напрямую")
    void activeListShouldNotBeMisreadAsPendingReview() {
        // Ровно тот сценарий бага: список активных заданий содержит CREATED-задания,
        // которые ребёнок ещё не отправлял — их нельзя посчитать как «На проверке».
        List<TaskAssignmentDto> activeAssignments = List.of(dto("CREATED"), dto("CREATED"));

        long pendingReviewCount = TaskAssignmentCounters.countByStatus(activeAssignments, "SUBMITTED");

        assertThat(pendingReviewCount).isZero();
        assertThat(activeAssignments).hasSize(2); // задания есть, но ни одно не сдано
    }
}
