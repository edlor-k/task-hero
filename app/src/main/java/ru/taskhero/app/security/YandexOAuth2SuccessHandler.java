package ru.taskhero.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import ru.taskhero.app.client.UserServiceClient;
import ru.taskhero.app.config.FeignConfig;
import ru.taskhero.app.dto.LoginResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class YandexOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserServiceClient userServiceClient;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();

        String email = oauthUser.getAttribute("default_email");
        String firstName = oauthUser.getAttribute("first_name");
        String lastName = oauthUser.getAttribute("last_name");

        if (email == null || email.isBlank()) {
            log.error("Yandex OAuth2: email не получен от провайдера");
            response.sendRedirect("/login?error=true");
            return;
        }

        try {
            Map<String, String> socialRequest = new HashMap<>();
            socialRequest.put("email", email);
            socialRequest.put("firstName", firstName != null ? firstName : "");
            socialRequest.put("surname", lastName != null ? lastName : "");

            LoginResponse loginResponse = userServiceClient.socialLogin(socialRequest);

            HttpSession session = request.getSession(true);
            session.setAttribute(FeignConfig.SESSION_TOKEN_KEY, loginResponse.token());

            AuthenticatedUser user = new AuthenticatedUser(email, "PARENT", loginResponse.token());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARENT")));

            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
            securityContextRepository.saveContext(ctx, request, response);

            log.info("Yandex SSO: успешный вход {}", email);
            response.sendRedirect("/parent/dashboard");

        } catch (Exception e) {
            log.error("Yandex SSO: ошибка входа для {}: {}", email, e.getMessage());
            response.sendRedirect("/login?error=true");
        }
    }
}
