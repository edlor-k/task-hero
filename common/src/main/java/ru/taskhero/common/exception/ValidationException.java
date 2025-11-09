package ru.taskhero.common.exception;

import lombok.Getter;

import java.util.Map;

/** Бизнес-валидация входных данных (возвращаем 400). */
@Getter
public class ValidationException extends RuntimeException {
    /** Список ошибок валидации. */
    private final Map<String, String> errors;

    /**
     * Базовый конструктор без списка ошибок.
     *
     * @param message сообщение об ошибке
     */
    public ValidationException(final String message) {
        super(message);
        this.errors = null;
    }

    /**
     * Конструктор с сообщением об ошибках и их списком.
     *
     * @param message сообщение об ошибке
     * @param errors список ошибок
     */
    public ValidationException(final String message, final Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
}
