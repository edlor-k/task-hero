package ru.taskhero.taskservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;
import ru.taskhero.common.model.enums.RepeatUnit;
import ru.taskhero.common.model.enums.TaskCategory;
import ru.taskhero.common.model.enums.TaskDifficulty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сущность шаблона задания.
 * Родители создают шаблоны, которые затем назначаются детям.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "task_templates")
public class TaskTemplate extends BaseEntity {

    /**
     * ID родителя, создавшего шаблон.
     */
    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    /**
     * Название задания.
     */
    @Column(name = "title", nullable = false, length = 128)
    private String title;

    /**
     * Подробное описание задания.
     */
    @Column(name = "description", length = 1024)
    private String description;

    /**
     * Награда в EXP за выполнение.
     */
    @Builder.Default
    @Column(name = "exp_reward", nullable = false)
    private int expReward = 10;

    /**
     * Награда в коинах за выполнение.
     */
    @Builder.Default
    @Column(name = "coins_reward", nullable = false)
    private int coinsReward = 5;

    /**
     * Повторяемое ли задание.
     */
    @Builder.Default
    @Column(name = "is_repeatable", nullable = false)
    private boolean repeatable = false;

    /**
     * Расписание повторения (DAILY, WEEKLY, MONTHLY или CRON).
     */
    @Column(name = "repeat_schedule", length = 64)
    private String repeatSchedule;

    /**
     * Количество повторений (N раз).
     */
    @Column(name = "repeat_count")
    private Integer repeatCount;

    /**
     * Интервал повторения (каждые M единиц).
     */
    @Column(name = "repeat_interval")
    private Integer repeatInterval;

    /**
     * Единица измерения интервала повторения.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_unit", length = 16)
    private RepeatUnit repeatUnit;

    /**
     * Правило повторения в формате RRULE (Google Calendar-style).
     * Примеры:
     * - "FREQ=DAILY" — каждый день
     * - "FREQ=WEEKLY;BYDAY=MO,WE,FR" — каждый пн, ср, пт
     * - "FREQ=MONTHLY;BYMONTHDAY=1,15" — 1-го и 15-го каждого месяца
     * - "FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,TH" — каждые 2 недели во вт и чт
     */
    @Column(name = "recurrence_rule", length = 256)
    private String recurrenceRule;

    /**
     * Категория задания (для библиотеки).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    private TaskCategory category;

    /**
     * Является ли библиотечным шаблоном (общедоступным примером).
     */
    @Builder.Default
    @Column(name = "is_library_template", nullable = false)
    private boolean libraryTemplate = false;

    /**
     * Уровень сложности задания.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 16)
    private TaskDifficulty difficulty = TaskDifficulty.NORMAL;

    /**
     * Активен ли шаблон (можно ли назначать задания на его основе).
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * Назначения заданий на основе этого шаблона.
     */
    @OneToMany(mappedBy = "template")
    @Builder.Default
    private List<TaskAssignment> assignments = new ArrayList<>();

    /**
     * Подпункты (подзадачи) шаблона.
     */
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<TaskSubItem> subItems = new ArrayList<>();
}
