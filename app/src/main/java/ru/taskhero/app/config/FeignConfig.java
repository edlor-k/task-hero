package ru.taskhero.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.InputStream;
import java.util.Map;

/**
 * Конфигурация Feign клиентов.
 * Передает JWT токен из сессии в запросы к API.
 */
@Slf4j
@Configuration
public class FeignConfig {

    public static final String SESSION_TOKEN_KEY = "jwt_token";

    /**
     * Уровень логирования Feign.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Интерцептор для добавления JWT токена к запросам.
     */
    @Bean
    public RequestInterceptor authRequestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpSession session = attributes.getRequest().getSession(false);
                if (session != null) {
                    String token = (String) session.getAttribute(SESSION_TOKEN_KEY);
                    if (token != null) {
                        requestTemplate.header("Authorization", "Bearer " + token);
                        log.debug("Added JWT token to Feign request");
                    }
                }
            }
        };
    }

    /**
     * Декодер ошибок Feign — извлекает читаемое сообщение
     * из тела ответа сервиса (ExceptionBody).
     */
    @Bean
    public ErrorDecoder feignErrorDecoder(
            ObjectMapper objectMapper
    ) {
        return (methodKey, response) -> {
            String message = getDefaultMessage(response.status());
            if (response.body() == null) {
                return new RuntimeException(message);
            }
            try (InputStream body =
                         response.body().asInputStream()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorBody =
                        objectMapper.readValue(body, Map.class);
                Object msg = errorBody.get("message");
                if (msg != null
                        && !msg.toString().isBlank()) {
                    message = msg.toString();
                }
                @SuppressWarnings("unchecked")
                Map<String, String> errors =
                        (Map<String, String>)
                                errorBody.get("errors");
                if (errors != null && !errors.isEmpty()) {
                    String details = String.join("; ",
                            errors.values());
                    message = message + ": " + details;
                }
            } catch (Exception ex) {
                log.warn(
                        "Could not parse error body "
                                + "for {}: {}",
                        methodKey, ex.getMessage());
            }
            return new RuntimeException(message);
        };
    }

    private static String getDefaultMessage(int status) {
        return switch (status) {
            case 400 -> "Ошибка валидации данных";
            case 401 -> "Ошибка аутентификации";
            case 403 -> "Доступ запрещён";
            case 404 -> "Ресурс не найден";
            default -> "Ошибка сервиса";
        };
    }
}

