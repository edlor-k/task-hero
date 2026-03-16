package ru.taskhero.taskservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Подпункт (подзадача) шаблона задания.
 * Используется для составных заданий.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "task_sub_items")
public class TaskSubItem extends BaseEntity {

    /**
     * Шаблон, к которому принадлежит подпункт.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private TaskTemplate template;

    /**
     * Название подпункта.
     */
    @Column(name = "title", nullable = false, length = 256)
    private String title;

    /**
     * Порядковый номер подпункта.
     */
    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;
}
