package ru.taskhero.app.security;

import feign.FeignException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.taskhero.app.client.UserServiceClient;
import ru.taskhero.app.config.FeignConfig;
import ru.taskhero.app.dto.AdminUserDto;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveUserSessionFilter extends OncePerRequestFilter {

    private final UserServiceClient userServiceClient;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!requiresActiveUserCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AdminUserDto currentUser = userServiceClient.getCurrentUser();
            if (!currentUser.active()) {
                invalidateSession(request);
                response.sendRedirect("/login?blocked=true");
                return;
            }
        } catch (FeignException.Unauthorized | FeignException.Forbidden ex) {
            invalidateSession(request);
            response.sendRedirect("/login?blocked=true");
            return;
        } catch (RuntimeException ex) {
            log.warn("Could not verify active user session: {}", ex.getMessage());
            invalidateSession(request);
            response.sendRedirect("/login?blocked=true");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresActiveUserCheck(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/parent/") && !uri.startsWith("/admin/")) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(FeignConfig.SESSION_TOKEN_KEY) != null;
    }

    private void invalidateSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ex) {
                log.debug("Session was already invalidated");
            }
        }
    }
}
