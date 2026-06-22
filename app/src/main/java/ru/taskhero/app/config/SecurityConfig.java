package ru.taskhero.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import ru.taskhero.app.security.ActiveUserSessionFilter;
import ru.taskhero.app.security.YandexOAuth2SuccessHandler;

/**
 * Конфигурация безопасности для веб-приложения.
 * Использует form-based аутентификацию с сессиями.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ActiveUserSessionFilter activeUserSessionFilter;
    private final YandexOAuth2SuccessHandler yandexOAuth2SuccessHandler;

    /**
     * Настройка цепочки фильтров безопасности.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Публичные страницы
                        .requestMatchers("/", "/home", "/about").permitAll()
                        .requestMatchers("/login", "/register", "/login/child").permitAll()
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Страницы для родителей
                        .requestMatchers("/parent/**").hasRole("PARENT")

                        // Страницы для детей
                        .requestMatchers("/child/**").hasRole("CHILD")

                        // Страницы для админов
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Все остальные требуют аутентификации
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("TASKHERO_SESSION")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(yandexOAuth2SuccessHandler)
                        .failureUrl("/login?error=true")
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/access-denied")
                )
                .addFilterAfter(activeUserSessionFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<ActiveUserSessionFilter> activeUserSessionFilterRegistration() {
        FilterRegistrationBean<ActiveUserSessionFilter> registration =
                new FilterRegistrationBean<>(activeUserSessionFilter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Обработчик успешной аутентификации.
     * Перенаправляет пользователя на соответствующий dashboard в зависимости от роли.
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority())
                    .orElse("");

            String redirectUrl = switch (role) {
                case "ROLE_PARENT" -> "/parent/dashboard";
                case "ROLE_CHILD" -> "/child/dashboard";
                case "ROLE_ADMIN" -> "/admin/dashboard";
                default -> "/";
            };

            response.sendRedirect(redirectUrl);
        };
    }

}
