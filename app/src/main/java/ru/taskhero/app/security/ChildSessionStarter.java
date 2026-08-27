package ru.taskhero.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import ru.taskhero.app.config.FeignConfig;
import ru.taskhero.app.dto.LoginResponse;

import java.util.Collections;

/**
 * Устанавливает аутентифицированную сессию ребёнка в текущем браузере.
 * <p>
 * Используется как при обычном входе ребёнка по токену ({@code /login/child}),
 * так и при удобном переходе «Открыть кабинет ребёнка» из кабинета родителя —
 * в обоих случаях набор действий (сохранить JWT в сессии, выставить SecurityContext
 * с ролью CHILD) идентичен, поэтому вынесен сюда, а не продублирован.
 */
@Component
public class ChildSessionStarter {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public void start(LoginResponse loginResponse, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        session.setAttribute(FeignConfig.SESSION_TOKEN_KEY, loginResponse.token());

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_CHILD");
        AuthenticatedUser user = new AuthenticatedUser(loginResponse.displayName(), "CHILD", loginResponse.token());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.singletonList(authority));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }
}
