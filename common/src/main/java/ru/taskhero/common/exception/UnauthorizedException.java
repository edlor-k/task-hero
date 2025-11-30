package ru.taskhero.common.exception;

/**
 * Исключение, выбрасываемое при попытке доступа к ресурсу без необходимых прав.
 * Обычно возвращает HTTP статус 403.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Конструктор с сообщением об ошибке.
     *
     * @param message описание ошибки
     */
    public UnauthorizedException(final String message) {
        super(message);
    }

    /**
     * Конструктор с сообщением и причиной.
     *
     * @param message описание ошибки
     * @param cause   причина исключения
     */
    public UnauthorizedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
