package ru.taskhero.common.exception;

import lombok.Getter;

import java.util.Map;

/** Бизнес-валидация входных данных (возвращаем 400). */
@Getter
public class ValidationException extends RuntimeException {
    private final Map<String, String> errors;

    /** Базовый конструктор без списка ошибок */
    public ValidationException(String message) {
        super(message);
        this.errors = null;
    }

    /** Конструктор с сообщением об ошибках и их списком */
    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
}
