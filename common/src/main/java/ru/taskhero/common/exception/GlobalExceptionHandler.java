package ru.taskhero.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений для всех REST контроллеров.
 * Перехватывает исключения и возвращает стандартизированные ответы.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка бизнес-валидации (ValidationException).
     * Возвращает HTTP 400 BAD REQUEST.
     *
     * @param e исключение валидации
     * @return тело ответа с ошибкой
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ExceptionBody> handleValidationException(final ValidationException e) {
        log.warn("Validation error: {}", e.getMessage());

        ExceptionBody body = ExceptionBody.builder()
                .message(e.getMessage())
                .errors(e.getErrors())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    /**
     * Обработка ошибок аутентификации (AuthenticationException).
     * Возвращает HTTP 401 UNAUTHORIZED.
     *
     * @param e исключение аутентификации
     * @return тело ответа с ошибкой
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ExceptionBody> handleAuthenticationException(final AuthenticationException e) {
        log.warn("Authentication error: {}", e.getMessage());

        ExceptionBody body = new ExceptionBody(e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(body);
    }

    /**
     * Обработка ошибок авторизации (UnauthorizedException).
     * Возвращает HTTP 403 FORBIDDEN.
     *
     * @param e исключение авторизации
     * @return тело ответа с ошибкой
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ExceptionBody> handleUnauthorizedException(final UnauthorizedException e) {
        log.warn("Authorization error: {}", e.getMessage());

        ExceptionBody body = new ExceptionBody(e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(body);
    }

    /**
     * Обработка ошибок "ресурс не найден" (ResourceNotFoundException).
     * Возвращает HTTP 404 NOT FOUND.
     *
     * @param e исключение "не найдено"
     * @return тело ответа с ошибкой
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionBody> handleResourceNotFoundException(final ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());

        ExceptionBody body = new ExceptionBody(e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(body);
    }

    /**
     * Обработка ошибок валидации Spring (@Valid).
     * Возвращает HTTP 400 BAD REQUEST с деталями ошибок валидации.
     *
     * @param e исключение валидации аргументов
     * @return тело ответа с ошибками валидации
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionBody> handleMethodArgumentNotValid(final MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ExceptionBody body = ExceptionBody.builder()
                .message("Ошибка валидации данных")
                .errors(errors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    /**
     * Обработка всех остальных исключений.
     * Возвращает HTTP 500 INTERNAL SERVER ERROR.
     *
     * @param e любое необработанное исключение
     * @return тело ответа с ошибкой
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionBody> handleGenericException(final Exception e) {
        log.error("Unexpected error occurred", e);

        ExceptionBody body = new ExceptionBody("Внутренняя ошибка сервера");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}
