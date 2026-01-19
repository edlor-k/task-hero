package ru.taskhero.common.exception;

/**
 * Исключение, выбрасываемое при ошибках аутентификации.
 * Обычно возвращает HTTP статус 401.
 */
public class AuthenticationException extends RuntimeException {

    /**
     * Конструктор с сообщением об ошибке.
     *
     * @param message описание ошибки
     */
    public AuthenticationException(final String message) {
        super(message);
    }

    /**
     * Конструктор с сообщением и причиной.
     *
     * @param message описание ошибки
     * @param cause   причина исключения
     */
    public AuthenticationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
