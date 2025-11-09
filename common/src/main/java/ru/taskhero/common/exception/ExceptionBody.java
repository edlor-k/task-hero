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
    private String message;
    private Map<String, String> errors;

    /**
     * Конструктор с одним сообщением.
     *
     * @param message текст ошибки
     */
    public ExceptionBody(String message) {
        this.message = message;
    }
}
