package ru.taskhero.taskservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Swagger/OpenAPI для Task Service.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Создает описание OpenAPI для Task Service с поддержкой JWT аутентификации.
     *
     * @return объект OpenAPI с метаданными API и схемой безопасности
     */
    @Bean
    public OpenAPI taskServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Service API")
                        .version("0.0.1")
                        .description("""
                                REST API для управления заданиями, шаблонами и наградами.
                                Входит в состав task-hero.
                                
                                Функционал:
                                - Создание и управление шаблонами заданий (PARENT)
                                - Назначение заданий детям (PARENT)
                                - Просмотр и сдача заданий (CHILD)
                                - Проверка и одобрение заданий (PARENT)
                                - Начисление EXP и коинов
                                
                                Для использования защищенных endpoints:
                                1. Получите JWT токен через User Service
                                2. Нажмите кнопку "Authorize" справа и вставьте токен
                                """)
                        .contact(new Contact()
                                .name("Vladlen Korablev")
                                .email("korablev.vlm@gmail.com")
                                .url("https://github.com/edlor-k/")))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Введите JWT токен, полученный при входе через User Service")
                        )
                );
    }
}
