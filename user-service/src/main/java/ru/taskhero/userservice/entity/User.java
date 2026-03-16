package ru.taskhero.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.taskhero.common.model.entity.BaseEntity;
import ru.taskhero.common.model.enums.Role;

/**
 * Сущность базового пользователя системы (аутентификация).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 128)
    private String email;

    @ToString.Exclude
    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
