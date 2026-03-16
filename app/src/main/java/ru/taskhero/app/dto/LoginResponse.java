package ru.taskhero.app.dto;

/**
 * DTO ответа на вход.
 * Поля соответствуют LoginResponseDto из user-service.
 */
public record LoginResponse(
        String accessToken,
        String displayName,
        String role
) {
    /**
     * Получить токен (alias для accessToken).
     */
    public String token() {
        return accessToken;
    }
}
