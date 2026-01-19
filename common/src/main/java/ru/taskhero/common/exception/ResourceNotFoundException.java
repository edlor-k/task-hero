package ru.taskhero.common.exception;

/**
 * Исключение, выбрасываемое когда запрашиваемый ресурс не найден.
 * Обычно возвращает HTTP статус 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Конструктор с сообщением об ошибке.
     *
     * @param message описание ошибки
     */
    public ResourceNotFoundException(final String message) {
        super(message);
    }

    /**
     * Конструктор с сообщением и причиной.
     *
     * @param message описание ошибки
     * @param cause   причина исключения
     */
    public ResourceNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
