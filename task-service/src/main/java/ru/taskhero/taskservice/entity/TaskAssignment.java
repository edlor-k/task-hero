package ru.taskhero.taskservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;
import ru.taskhero.common.model.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Сущность назначенного задания.
 * Связывает шаблон задания с конкретным ребёнком.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "task_assignments")
public class TaskAssignment extends BaseEntity {

    /**
     * Шаблон, на основе которого создано задание.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private TaskTemplate template;

    /**
     * ID ребёнка, которому назначено задание.
     */
    @Column(name = "child_id", nullable = false)
    private UUID childId;

    /**
     * Текущий статус задания.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status = TaskStatus.CREATED;

    /**
     * Срок выполнения задания.
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Комментарий ребёнка при сдаче задания.
     */
    @Column(name = "child_comment", length = 512)
    private String childComment;

    /**
     * Комментарий родителя при проверке задания.
     */
    @Column(name = "parent_comment", length = 512)
    private String parentComment;

    /**
     * Время, когда ребёнок сдал задание на проверку.
     */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    /**
     * Время, когда родитель проверил задание.
     */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /**
     * EXP, начисленный за это задание (может отличаться от шаблона).
     */
    @Column(name = "exp_earned")
    private Integer expEarned;

    /**
     * Коины, начисленные за это задание (могут отличаться от шаблона).
     */
    @Column(name = "coins_earned")
    private Integer coinsEarned;
}
