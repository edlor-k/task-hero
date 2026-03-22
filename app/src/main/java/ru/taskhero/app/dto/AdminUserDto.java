package ru.taskhero.app.dto;

import java.util.UUID;

/**
 * DTO пользователя (из user-service admin API).
 */
public record AdminUserDto(
        UUID id,
        String email,
        String role,
        boolean isActive
) {
    /**
     * Alias для isActive (для Thymeleaf).
     */
    public boolean active() {
        return isActive;
    }
}
