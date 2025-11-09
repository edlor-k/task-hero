package ru.taskhero.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Конфигурация JPA-аудита.
 * Активирует автоматическое заполнение полей @CreatedDate и @LastModifiedDate.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
