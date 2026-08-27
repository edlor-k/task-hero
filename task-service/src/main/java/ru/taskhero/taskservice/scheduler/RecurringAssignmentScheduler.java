package ru.taskhero.taskservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.entity.TaskAssignment;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.repository.TaskAssignmentRepository;
import ru.taskhero.taskservice.repository.TaskTemplateRepository;
import ru.taskhero.taskservice.util.RecurrenceHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Планировщик автоматической генерации очередных назначений для повторяющихся заданий.
 * <p>
 * До этого класса {@link TaskTemplate#getRecurrenceRule()} только хранился в базе —
 * ничего не создавало следующее назначение, поэтому «повторяющиеся задания» на деле
 * не повторялись без ручного вмешательства родителя.
 * <p>
 * Точка отсчёта для правила повторения — дата самого первого назначения этого шаблона
 * этому ребёнку. Каждый прогон планировщика проверяет только «сегодняшнее» вхождение
 * правила и создаёт для него новое задание, если оно ещё не создано — это исключает
 * двойное создание при повторных запусках.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringAssignmentScheduler {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final int MAX_LOOKBACK_DAYS = 366;

    private final TaskTemplateRepository templateRepository;
    private final TaskAssignmentRepository assignmentRepository;

    /**
     * Раз в час проверяет все повторяющиеся шаблоны и создаёт задания на сегодня,
     * если по правилу повторения они уже должны появиться и ещё не были созданы.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void generateDueRecurringAssignments() {
        LocalDate today = LocalDate.now(ZONE);
        List<TaskTemplate> recurringTemplates = templateRepository
                .findAllByActiveTrueAndRepeatableTrueAndRecurrenceRuleIsNotNull();

        int created = 0;
        for (TaskTemplate template : recurringTemplates) {
            for (UUID childId : assignmentRepository.findDistinctChildIdsByTemplateId(template.getId())) {
                if (generateForChildIfDue(template, childId, today)) {
                    created++;
                }
            }
        }

        if (created > 0) {
            log.info("Автоматически создано {} назначений по повторяющимся шаблонам", created);
        }
    }

    private boolean generateForChildIfDue(TaskTemplate template, UUID childId, LocalDate today) {
        TaskAssignment firstAssignment = assignmentRepository
                .findFirstByTemplateIdAndChildIdOrderByCreatedAtAsc(template.getId(), childId)
                .orElse(null);
        if (firstAssignment == null) {
            return false;
        }

        LocalDate anchor = firstAssignment.getCreatedAt().atZone(ZONE).toLocalDate();
        if (anchor.isAfter(today)) {
            return false;
        }

        int lookbackDays = (int) Math.min(ChronoUnit.DAYS.between(anchor, today) + 1, MAX_LOOKBACK_DAYS);
        List<LocalDate> occurrences = RecurrenceHelper.getNextOccurrences(
                template.getRecurrenceRule(), anchor, lookbackDays);

        boolean isDueToday = occurrences.stream().anyMatch(today::isEqual);
        if (!isDueToday) {
            return false;
        }

        Instant startOfDay = today.atStartOfDay(ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZONE).toInstant();
        boolean alreadyExists = assignmentRepository.existsByTemplateIdAndChildIdAndDueDateBetween(
                template.getId(), childId, startOfDay, endOfDay)
                || assignmentRepository.existsByTemplateIdAndChildIdAndDueDateIsNullAndCreatedAtBetween(
                template.getId(), childId, startOfDay, endOfDay);
        if (alreadyExists) {
            return false;
        }

        Instant dueDate = today.atTime(LocalTime.of(23, 59, 59)).atZone(ZONE).toInstant();
        TaskAssignment nextAssignment = TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.CREATED)
                .dueDate(dueDate)
                .build();

        assignmentRepository.save(nextAssignment);
        log.info("Создано повторяющееся назначение {} по шаблону {} для ребёнка {} на {}",
                nextAssignment.getId(), template.getId(), childId, today);
        return true;
    }
}
