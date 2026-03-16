package ru.taskhero.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;
import ru.taskhero.common.model.enums.CharacterType;
import ru.taskhero.common.model.enums.DifficultyTrajectory;

import java.util.UUID;

/**
 * Сущность профиля ребёнка.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "children")
public class Child extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Column(name = "first_name", nullable = false, length = 64)
    private String firstName;

    @Column(name = "surname", nullable = false, length = 64)
    private String surname;


    @Builder.Default
    @Column(name = "exp", nullable = false)
    private int exp = 0;

    @Builder.Default
    @Column(name = "coins", nullable = false)
    private int coins = 0;

    @Builder.Default
    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "avatar_url", length = 256)
    private String avatarUrl;

    @Column(name = "login_token", unique = true, length = 64)
    private String loginToken;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_trajectory", nullable = false, length = 16)
    private DifficultyTrajectory difficultyTrajectory = DifficultyTrajectory.NORMAL;

    /**
     * Тип персонажа (выбирается ребёнком при первом входе).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "character_type", length = 16)
    private CharacterType characterType;

    /**
     * Флаг, выбран ли персонаж (при первом входе).
     */
    @Builder.Default
    @Column(name = "character_selected", nullable = false)
    private boolean characterSelected = false;
}
