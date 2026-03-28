package ru.taskhero.taskservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.entity.TaskAssignment;
import ru.taskhero.taskservice.repository.TaskAssignmentRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Планировщик для автоматического истечения просроченных заданий.
 * Проверяет задания с дедлайном, который уже прошёл, и переводит их в статус EXPIRED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineExpirationScheduler {

    private final TaskAssignmentRepository assignmentRepository;

    /**
     * Каждый час проверяет просроченные задания и помечает их как EXPIRED.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void expireOverdueTasks() {
        LocalDateTime today = LocalDateTime.now();
        List<TaskAssignment> overdue = assignmentRepository
                .findAllByDueDateBeforeAndStatus(today, TaskStatus.CREATED);

        if (!overdue.isEmpty()) {
            log.info("Найдено {} просроченных заданий, переводим в EXPIRED", overdue.size());
            for (TaskAssignment assignment : overdue) {
                assignment.setStatus(TaskStatus.EXPIRED);
            }
            assignmentRepository.saveAll(overdue);
        }
    }
}
