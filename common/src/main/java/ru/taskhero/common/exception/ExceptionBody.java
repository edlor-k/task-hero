package ru.taskhero.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Тело ответа при обработке исключений.
 */
@Data
@AllArgsConstructor
@Builder
public class ExceptionBody {
    /** Сообщение об ошибке. */
    private String message;
    /** Список ошибок. */
    private Map<String, String> errors;

    /**
     * Конструктор с одним сообщением.
     *
     * @param message текст ошибки
     */
    public ExceptionBody(final String message) {
        this.message = message;
    }
}
