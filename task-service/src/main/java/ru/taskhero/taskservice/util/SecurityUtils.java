package ru.taskhero.taskservice.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.taskhero.common.exception.AuthenticationException;

import java.util.UUID;

/**
 * Утилитарный класс для работы с контекстом безопасности.
 */
@UtilityClass
public class SecurityUtils {

    /**
     * Получить ID текущего аутентифицированного пользователя.
     *
     * @return UUID пользователя
     * @throws AuthenticationException если пользователь не аутентифицирован
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("Пользователь не аутентифицирован");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UUID) {
            return (UUID) principal;
        }

        throw new AuthenticationException("Невозможно получить ID пользователя из токена");
    }

    /**
     * Получить роль текущего пользователя.
     *
     * @return роль пользователя (PARENT, CHILD, ADMIN)
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("Пользователь не аутентифицирован");
        }

        return authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElseThrow(() -> new AuthenticationException("Роль пользователя не найдена"));
    }

    /**
     * Проверить, является ли текущий пользователь родителем.
     *
     * @return true если пользователь имеет роль PARENT
     */
    public static boolean isParent() {
        return "PARENT".equals(getCurrentUserRole());
    }

    /**
     * Проверить, является ли текущий пользователь ребёнком.
     *
     * @return true если пользователь имеет роль CHILD
     */
    public static boolean isChild() {
        return "CHILD".equals(getCurrentUserRole());
    }

    /**
     * Проверить, является ли текущий пользователь администратором.
     *
     * @return true если пользователь имеет роль ADMIN
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }
}
