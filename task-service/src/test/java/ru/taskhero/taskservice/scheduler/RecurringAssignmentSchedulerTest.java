package ru.taskhero.taskservice.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.entity.TaskAssignment;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.repository.TaskAssignmentRepository;
import ru.taskhero.taskservice.repository.TaskTemplateRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecurringAssignmentScheduler Unit Tests")
class RecurringAssignmentSchedulerTest {

    @Mock
    private TaskTemplateRepository templateRepository;

    @Mock
    private TaskAssignmentRepository assignmentRepository;

    private RecurringAssignmentScheduler scheduler;

    private UUID childId;
    private TaskTemplate dailyTemplate;

    @BeforeEach
    void setUp() {
        scheduler = new RecurringAssignmentScheduler(templateRepository, assignmentRepository);
        childId = UUID.randomUUID();
        dailyTemplate = TaskTemplate.builder()
                .parentId(UUID.randomUUID())
                .title("Зарядка")
                .expReward(10)
                .coinsReward(5)
                .repeatable(true)
                .active(true)
                .recurrenceRule("FREQ=DAILY")
                .build();
    }

    private TaskAssignment firstAssignmentAnchoredDaysAgo(int daysAgo) {
        return TaskAssignment.builder()
                .template(dailyTemplate)
                .childId(childId)
                .status(TaskStatus.APPROVED)
                .createdAt(LocalDate.now(ZoneId.systemDefault()).minusDays(daysAgo)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant())
                .build();
    }

    @Test
    @DisplayName("Должен создать очередное назначение для ежедневного задания, если сегодняшнего ещё нет")
    void shouldCreateNextOccurrenceWhenDue() {
        // Given
        when(templateRepository.findAllByActiveTrueAndRepeatableTrueAndRecurrenceRuleIsNotNull())
                .thenReturn(List.of(dailyTemplate));
        when(assignmentRepository.findDistinctChildIdsByTemplateId(dailyTemplate.getId()))
                .thenReturn(List.of(childId));
        when(assignmentRepository.findFirstByTemplateIdAndChildIdOrderByCreatedAtAsc(dailyTemplate.getId(), childId))
                .thenReturn(Optional.of(firstAssignmentAnchoredDaysAgo(3)));
        when(assignmentRepository.existsByTemplateIdAndChildIdAndDueDateBetween(any(), any(), any(), any()))
                .thenReturn(false);
        when(assignmentRepository.existsByTemplateIdAndChildIdAndDueDateIsNullAndCreatedAtBetween(any(), any(), any(), any()))
                .thenReturn(false);

        // When
        scheduler.generateDueRecurringAssignments();

        // Then
        ArgumentCaptor<TaskAssignment> captor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        TaskAssignment created = captor.getValue();
        assertThat(created.getChildId()).isEqualTo(childId);
        assertThat(created.getStatus()).isEqualTo(TaskStatus.CREATED);
        assertThat(created.getTemplate()).isEqualTo(dailyTemplate);
        assertThat(created.getDueDate()).isNotNull();
    }

    @Test
    @DisplayName("Не должен создавать второе назначение на тот же день (защита от двойного создания)")
    void shouldNotCreateDuplicateForSameDay() {
        // Given
        when(templateRepository.findAllByActiveTrueAndRepeatableTrueAndRecurrenceRuleIsNotNull())
                .thenReturn(List.of(dailyTemplate));
        when(assignmentRepository.findDistinctChildIdsByTemplateId(dailyTemplate.getId()))
                .thenReturn(List.of(childId));
        when(assignmentRepository.findFirstByTemplateIdAndChildIdOrderByCreatedAtAsc(dailyTemplate.getId(), childId))
                .thenReturn(Optional.of(firstAssignmentAnchoredDaysAgo(3)));
        when(assignmentRepository.existsByTemplateIdAndChildIdAndDueDateBetween(any(), any(), any(), any()))
                .thenReturn(true);

        // When
        scheduler.generateDueRecurringAssignments();

        // Then
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Не должен создавать назначение, если ребёнку ещё ни разу не назначался этот шаблон")
    void shouldSkipChildWithoutAnyPriorAssignment() {
        // Given
        when(templateRepository.findAllByActiveTrueAndRepeatableTrueAndRecurrenceRuleIsNotNull())
                .thenReturn(List.of(dailyTemplate));
        when(assignmentRepository.findDistinctChildIdsByTemplateId(dailyTemplate.getId()))
                .thenReturn(List.of(childId));
        when(assignmentRepository.findFirstByTemplateIdAndChildIdOrderByCreatedAtAsc(dailyTemplate.getId(), childId))
                .thenReturn(Optional.empty());

        // When
        scheduler.generateDueRecurringAssignments();

        // Then
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Не должен создавать назначение для дня, не подходящего под правило (по будням в выходной)")
    void shouldNotCreateWhenRuleDoesNotMatchToday() {
        // Given — правило «по будням», а сегодня выпадает на выходной или нет — подстрахуемся:
        // возьмём правило, которое точно не совпадёт ни с одним днём диапазона (несуществующий FREQ).
        TaskTemplate weirdTemplate = TaskTemplate.builder()
                .parentId(UUID.randomUUID())
                .title("Странное правило")
                .expReward(10)
                .coinsReward(5)
                .repeatable(true)
                .active(true)
                .recurrenceRule("FREQ=YEARLY")
                .build();

        when(templateRepository.findAllByActiveTrueAndRepeatableTrueAndRecurrenceRuleIsNotNull())
                .thenReturn(List.of(weirdTemplate));
        when(assignmentRepository.findDistinctChildIdsByTemplateId(weirdTemplate.getId()))
                .thenReturn(List.of(childId));
        when(assignmentRepository.findFirstByTemplateIdAndChildIdOrderByCreatedAtAsc(weirdTemplate.getId(), childId))
                .thenReturn(Optional.of(TaskAssignment.builder()
                        .template(weirdTemplate)
                        .childId(childId)
                        .status(TaskStatus.APPROVED)
                        .createdAt(LocalDate.now(ZoneId.systemDefault()).minusDays(3)
                                .atStartOfDay(ZoneId.systemDefault()).toInstant())
                        .build()));

        // When
        scheduler.generateDueRecurringAssignments();

        // Then
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Не должен дублировать назначение, если первое (без дедлайна) уже создано сегодня")
    void shouldNotDuplicateWhenFirstAssignmentCreatedTodayWithoutDueDate() {
        // Given — шаблон и первое назначение созданы только что, без дедлайна (типичный сценарий
        // выбора пресета повторения в форме назначения)
        Instant now = Instant.now();
        TaskAssignment firstToday = TaskAssignment.builder()
                .template(dailyTemplate)
                .childId(childId)
                .status(TaskStatus.CREATED)
                .createdAt(now)
                .build();

        when(templateRepository.findAllByActiveTrueAndRepeatableTrueAndRecurrenceRuleIsNotNull())
                .thenReturn(List.of(dailyTemplate));
        when(assignmentRepository.findDistinctChildIdsByTemplateId(dailyTemplate.getId()))
                .thenReturn(List.of(childId));
        when(assignmentRepository.findFirstByTemplateIdAndChildIdOrderByCreatedAtAsc(dailyTemplate.getId(), childId))
                .thenReturn(Optional.of(firstToday));
        when(assignmentRepository.existsByTemplateIdAndChildIdAndDueDateBetween(any(), any(), any(), any()))
                .thenReturn(false);
        when(assignmentRepository.existsByTemplateIdAndChildIdAndDueDateIsNullAndCreatedAtBetween(any(), any(), any(), any()))
                .thenReturn(true);

        // When
        scheduler.generateDueRecurringAssignments();

        // Then
        verify(assignmentRepository, never()).save(any());
    }
}
