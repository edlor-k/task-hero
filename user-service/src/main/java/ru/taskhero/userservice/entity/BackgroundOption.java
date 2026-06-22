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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "background_options")
public class BackgroundOption extends BaseEntity {

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(name = "image_key", nullable = false, length = 256)
    private String imageKey;

    @Column(name = "label", length = 64)
    private String label;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
