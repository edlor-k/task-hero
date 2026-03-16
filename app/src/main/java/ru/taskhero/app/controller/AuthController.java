package ru.taskhero.app.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.taskhero.app.client.UserServiceClient;
import ru.taskhero.app.config.FeignConfig;
import ru.taskhero.app.dto.LoginResponse;
import ru.taskhero.app.dto.RegisterRequest;
import ru.taskhero.app.security.AuthenticatedUser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для аутентификации и регистрации.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserServiceClient userServiceClient;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /**
     * Страница входа.
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("error", "Неверный email или пароль");
        }
        if (logout != null) {
            model.addAttribute("message", "Вы успешно вышли из системы");
        }
        return "auth/login";
    }

    /**
     * Страница входа для ребёнка.
     */
    @GetMapping("/login/child")
    public String childLoginPage() {
        return "auth/child-login";
    }

    /**
     * Обработка входа ребёнка по токену.
     */
    @PostMapping("/login/child")
    public String childLogin(
            @RequestParam String loginToken,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("loginToken", loginToken);

            LoginResponse loginResponse = userServiceClient.loginChild(loginRequest);

            // Сохраняем токен в сессии
            HttpSession session = request.getSession(true);
            session.setAttribute(FeignConfig.SESSION_TOKEN_KEY, loginResponse.token());

            // Устанавливаем аутентификацию
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_CHILD");
            AuthenticatedUser user = new AuthenticatedUser(loginResponse.displayName(), "CHILD", loginResponse.token());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, Collections.singletonList(authority));

            // Создаём новый SecurityContext и сохраняем его в сессию
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(auth);
            SecurityContextHolder.setContext(securityContext);
            securityContextRepository.saveContext(securityContext, request, response);

            log.info("Child logged in successfully: {}", loginResponse.displayName());

            return "redirect:/child/dashboard";

        } catch (Exception e) {
            log.error("Child login failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Неверный токен входа");
            return "redirect:/login/child";
        }
    }

    /**
     * Страница регистрации.
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest("", "", "", "", ""));
        return "auth/register";
    }

    /**
     * Обработка регистрации.
     */
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegisterRequest request,
            BindingResult bindingResult,
            HttpServletRequest req,
            HttpServletResponse resp,
            Model model
    ) {
        // Проверка валидации
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        // Проверка совпадения паролей
        if (!request.password().equals(request.confirmPassword())) {
            model.addAttribute("error", "Пароли не совпадают");
            return "auth/register";
        }

        try {
            Map<String, String> registerData = new HashMap<>();
            registerData.put("email", request.email());
            registerData.put("password", request.password());
            registerData.put("firstName", request.firstName());
            registerData.put("surname", request.surname());

            userServiceClient.register(registerData);
            log.info("User registered successfully: {}", request.email());

            // Автоматически входим после регистрации
            LoginResponse loginResponse = userServiceClient.login(
                    new ru.taskhero.app.dto.LoginRequest(request.email(), request.password()));

            HttpSession session = req.getSession(true);
            session.setAttribute(FeignConfig.SESSION_TOKEN_KEY, loginResponse.token());

            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_PARENT");
            AuthenticatedUser user = new AuthenticatedUser(
                    loginResponse.displayName(), "PARENT", loginResponse.token());
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, Collections.singletonList(authority));

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(auth);
            SecurityContextHolder.setContext(securityContext);
            securityContextRepository.saveContext(securityContext, req, resp);

            return "redirect:/parent/onboarding";

        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            model.addAttribute("error", "Ошибка регистрации: " + e.getMessage());
            return "auth/register";
        }
    }
}
