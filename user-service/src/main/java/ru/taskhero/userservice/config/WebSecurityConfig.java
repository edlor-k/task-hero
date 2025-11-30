package ru.taskhero.userservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.taskhero.userservice.security.JwtAuthenticationFilter;

import java.util.List;

/**
 * Конфигурация безопасности для user-service.
 * Настраивает JWT аутентификацию, CORS и правила доступа к эндпоинтам.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Настройка цепочки фильтров безопасности.
     *
     * @param http объект конфигурации HTTP безопасности
     * @return настроенная цепочка фильтров
     * @throws Exception если возникла ошибка конфигурации
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Отключаем CSRF так как используем stateless JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Настраиваем CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Настраиваем правила авторизации
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты (регистрация и вход)
                        .requestMatchers("/auth/**").permitAll()

                        // Swagger UI и API docs
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                        // Actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()

                        // Все остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                )

                // Stateless сессии (не храним состояние на сервере)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Добавляем наш JWT фильтр перед стандартным фильтром аутентификации
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Настройка CORS для разрешения запросов с фронтенда.
     *
     * @return источник конфигурации CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Разрешённые origins (в продакшене нужно указать конкретные домены)
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200"));

        // Разрешённые HTTP методы
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Разрешённые заголовки
        configuration.setAllowedHeaders(List.of("*"));

        // Разрешить credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Время кеширования preflight запросов
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
