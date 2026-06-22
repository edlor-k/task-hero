package ru.taskhero.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;

/**
 * Картинка-аватар из галереи, которую загружает администратор в объектное
 * хранилище. Ребёнок выбирает себе один аватар из активных опций.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "avatar_options")
public class AvatarOption extends BaseEntity {

    /**
     * Публичный URL картинки в бакете — отдаётся клиенту напрямую.
     */
    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    /**
     * Ключ объекта в бакете (например, {@code avatars/<uuid>.png}) — нужен,
     * чтобы удалить файл из хранилища при удалении опции.
     */
    @Column(name = "image_key", nullable = false, length = 256)
    private String imageKey;

    /**
     * Необязательное отображаемое название для админки (например, "Дракон").
     */
    @Column(name = "label", length = 64)
    private String label;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
