package ru.taskhero.taskservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Конфигурация Feign клиентов для взаимодействия с другими сервисами.
 */
@Slf4j
@Configuration
public class FeignConfig {

    /**
     * Уровень логирования Feign клиента.
     *
     * @return уровень логирования
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Интерцептор для передачи JWT токена в запросах к другим сервисам.
     * Копирует Authorization header из текущего запроса.
     *
     * @return интерцептор запросов
     */
    @Bean
    public RequestInterceptor authRequestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                    log.debug("Forwarding Authorization header to Feign request");
                }
            }
        };
    }
}
