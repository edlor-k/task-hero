package ru.taskhero.app.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Представление аутентифицированного пользователя.
 * Хранится как principal в SecurityContext.
 */
@Data
@AllArgsConstructor
public class AuthenticatedUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private String email;
    private String role;
    private String token;

    /**
     * Является ли пользователь родителем.
     */
    public boolean isParent() {
        return "PARENT".equals(role);
    }

    /**
     * Является ли пользователь ребёнком.
     */
    public boolean isChild() {
        return "CHILD".equals(role);
    }

    /**
     * Является ли пользователь администратором.
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
