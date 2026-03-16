package ru.taskhero.app.security;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.taskhero.app.client.UserServiceClient;
import ru.taskhero.app.config.FeignConfig;
import ru.taskhero.app.dto.LoginRequest;
import ru.taskhero.app.dto.LoginResponse;

import java.util.Collections;

/**
 * Кастомный провайдер аутентификации.
 * Вызывает user-service для проверки учетных данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserServiceClient userServiceClient;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        log.info("Attempting to authenticate user: {}", email);

        try {
            // Вызываем user-service для аутентификации
            LoginRequest request = new LoginRequest(email, password);
            LoginResponse response = userServiceClient.login(request);

            // Сохраняем токен в сессии
            saveTokenToSession(response.token());

            // Создаем аутентификацию
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + response.role());

            log.info("User {} authenticated successfully with role {}", email, response.role());

            return new UsernamePasswordAuthenticationToken(
                    new AuthenticatedUser(response.displayName(), response.role(), response.token()),
                    null,
                    Collections.singletonList(authority)
            );

        } catch (Exception e) {
            log.error("Authentication failed for user {}: {}", email, e.getMessage());
            throw new BadCredentialsException("Неверный email или пароль");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Сохранить JWT токен в HTTP сессию.
     */
    private void saveTokenToSession(String token) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpSession session = attributes.getRequest().getSession(true);
            session.setAttribute(FeignConfig.SESSION_TOKEN_KEY, token);
            log.debug("JWT token saved to session");
        }
    }
}
