package ru.taskhero.taskservice.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.entity.TaskAssignment;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.repository.TaskAssignmentRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeadlineExpirationScheduler Unit Tests")
class DeadlineExpirationSchedulerTest {

    @Mock
    private TaskAssignmentRepository assignmentRepository;

    @InjectMocks
    private DeadlineExpirationScheduler scheduler;

    @Test
    @DisplayName("Должен перевести просроченные задания в статус EXPIRED")
    void shouldExpireOverdueTasks() {
        // Given
        TaskTemplate template = TaskTemplate.builder()
                .parentId(UUID.randomUUID())
                .title("Просроченное задание")
                .expReward(10)
                .coinsReward(5)
                .build();

        TaskAssignment overdue1 = TaskAssignment.builder()
                .template(template)
                .childId(UUID.randomUUID())
                .status(TaskStatus.CREATED)
                .dueDate(Instant.now().minus(Duration.ofDays(1)))
                .build();

        TaskAssignment overdue2 = TaskAssignment.builder()
                .template(template)
                .childId(UUID.randomUUID())
                .status(TaskStatus.CREATED)
                .dueDate(Instant.now().minus(Duration.ofHours(2)))
                .build();

        when(assignmentRepository.findAllByDueDateBeforeAndStatus(any(Instant.class), eq(TaskStatus.CREATED)))
                .thenReturn(List.of(overdue1, overdue2));
        when(assignmentRepository.saveAll(any())).thenReturn(List.of(overdue1, overdue2));

        // When
        scheduler.expireOverdueTasks();

        // Then
        assertThat(overdue1.getStatus()).isEqualTo(TaskStatus.EXPIRED);
        assertThat(overdue2.getStatus()).isEqualTo(TaskStatus.EXPIRED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assignmentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("Не должен вызывать saveAll при отсутствии просроченных заданий")
    void shouldNotSaveWhenNoOverdueTasks() {
        // Given
        when(assignmentRepository.findAllByDueDateBeforeAndStatus(any(Instant.class), eq(TaskStatus.CREATED)))
                .thenReturn(List.of());

        // When
        scheduler.expireOverdueTasks();

        // Then
        verify(assignmentRepository).findAllByDueDateBeforeAndStatus(any(), any());
        verifyNoMoreInteractions(assignmentRepository);
    }

    @Test
    @DisplayName("Не должен трогать задания без дедлайна")
    void shouldNotExpireTasksWithoutDueDate() {
        // Given — репозиторий возвращает только задания с прошедшим дедлайном
        when(assignmentRepository.findAllByDueDateBeforeAndStatus(any(Instant.class), eq(TaskStatus.CREATED)))
                .thenReturn(List.of());

        // When
        scheduler.expireOverdueTasks();

        // Then
        verify(assignmentRepository).findAllByDueDateBeforeAndStatus(any(), eq(TaskStatus.CREATED));
        verifyNoMoreInteractions(assignmentRepository);
    }
}
