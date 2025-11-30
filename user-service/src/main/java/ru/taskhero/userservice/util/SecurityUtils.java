package ru.taskhero.userservice.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.taskhero.common.exception.UnauthorizedException;

import java.util.UUID;

/**
 * Утилитный класс для работы с контекстом безопасности Spring Security.
 */
@UtilityClass
public class SecurityUtils {

    /**
     * Получение ID текущего аутентифицированного пользователя.
     *
     * @return UUID пользователя
     * @throws UnauthorizedException если пользователь не аутентифицирован
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Пользователь не аутентифицирован");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UUID uuid) {
            return uuid;
        }

        throw new UnauthorizedException("Невозможно определить ID пользователя");
    }

    /**
     * Получение роли текущего пользователя.
     *
     * @return строка с ролью (например, "PARENT", "CHILD", "ADMIN")
     * @throws UnauthorizedException если пользователь не аутентифицирован
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Пользователь не аутентифицирован");
        }

        return authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElseThrow(() -> new UnauthorizedException("Роль пользователя не определена"));
    }

    /**
     * Проверка, является ли текущий пользователь родителем.
     *
     * @return true если роль PARENT
     */
    public static boolean isParent() {
        try {
            return "PARENT".equals(getCurrentUserRole());
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    /**
     * Проверка, является ли текущий пользователь ребёнком.
     *
     * @return true если роль CHILD
     */
    public static boolean isChild() {
        try {
            return "CHILD".equals(getCurrentUserRole());
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    /**
     * Проверка, является ли текущий пользователь администратором.
     *
     * @return true если роль ADMIN
     */
    public static boolean isAdmin() {
        try {
            return "ADMIN".equals(getCurrentUserRole());
        } catch (UnauthorizedException e) {
            return false;
        }
    }
}
