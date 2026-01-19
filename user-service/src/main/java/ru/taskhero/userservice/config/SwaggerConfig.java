package ru.taskhero.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Swagger/OpenAPI для User Service.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Создает описание OpenAPI для User Service с поддержкой JWT аутентификации.
     *
     * @return объект OpenAPI c метаданными API и схемой безопасности
     */
    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .version("0.0.1")
                        .description("""
                                REST API для управления пользователями и аутентификации.
                                Входит в состав task-hero.
                                
                                Для использования защищенных endpoints:
                                1. Зарегистрируйтесь через POST /auth/register
                                2. Войдите через POST /auth/login
                                3. Скопируйте accessToken из ответа
                                4. Нажмите кнопку "Authorize" справа и вставьте токен
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
                                        .description("Введите JWT токен, полученный при входе")
                        )
                );
    }
}
