package ru.taskhero.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;
import ru.taskhero.common.model.enums.DifficultyTrajectory;


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

    @Column(name = "login_token", unique = true, length = 64)
    private String loginToken;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_trajectory", nullable = false, length = 16)
    private DifficultyTrajectory difficultyTrajectory = DifficultyTrajectory.NORMAL;

    /**
     * Аватар ребёнка, выбранный из галереи (картинки в объектном хранилище).
     * {@code null}, пока ребёнок не выбрал аватар при первом входе.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_option_id")
    private AvatarOption avatarOption;

    /**
     * Фон ребёнка, выбранный из галереи. {@code null}, пока ребёнок не выбрал фон.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_option_id")
    private BackgroundOption backgroundOption;

    /**
     * Акцентный цвет UI ребёнка (ключ палитры: primary, success, accent, danger, pink, cyan, orange, indigo).
     * {@code null} соответствует дефолту — primary (фиолетовый).
     */
    @Column(name = "accent_color", length = 20)
    private String accentColor;

    /**
     * Никнейм ребёнка (отображается вместо имени в публичном UI).
     */
    @Column(name = "nickname", length = 64)
    private String nickname;

    /**
     * Разблокировано ли изменение никнейма (открывается после выполнения первого задания).
     */
    @Builder.Default
    @Column(name = "nickname_unlocked", nullable = false)
    private boolean nicknameUnlocked = false;
}
