package ru.taskhero.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Конфигурация безопасности, предоставляющая общий PasswordEncoder
 * для всех модулей системы (например, user-service).
 */
@Configuration
public class SecurityConfig {

    /**
     * Бин для шифрования паролей.
     * Используется при регистрации и аутентификации пользователей.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
