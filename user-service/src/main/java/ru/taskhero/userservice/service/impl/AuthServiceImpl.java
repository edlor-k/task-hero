package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.aop.LogMethod;
import ru.taskhero.common.exception.AuthenticationException;
import ru.taskhero.userservice.dto.ChildLoginRequestDto;
import ru.taskhero.userservice.dto.LoginResponseDto;
import ru.taskhero.userservice.dto.UserLoginRequestDto;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.UserRepository;
import ru.taskhero.userservice.security.JwtTokenProvider;
import ru.taskhero.userservice.service.AuthService;

/**
 * Имплементация AuthService для аутентификации пользователей.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Авторизация родителя или администратора.
     *
     * @param request данные для входа (email + password)
     * @return DTO с JWT токеном и информацией о пользователе
     * @throws AuthenticationException если учетные данные неверны
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod(value = "auth-login-user", logArgs = false)
    public LoginResponseDto loginUser(UserLoginRequestDto request) {
        // Поиск пользователя по email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Попытка входа с несуществующим email: {}", request.email());
                    return new AuthenticationException("Неверный email или пароль");
                });

        // Проверка активности пользователя
        if (!user.isActive()) {
            log.warn("Попытка входа заблокированного пользователя: {}", request.email());
            throw new AuthenticationException("Учетная запись заблокирована");
        }

        // Проверка пароля
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Неверный пароль для пользователя: {}", request.email());
            throw new AuthenticationException("Неверный email или пароль");
        }

        // Генерация JWT токена
        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        log.info("Успешная авторизация пользователя: {}", user.getEmail());

        return new LoginResponseDto(
                token,
                user.getEmail(),
                user.getRole().name()
        );
    }

    /**
     * Авторизация ребёнка по loginToken.
     *
     * @param request данные для входа (loginToken)
     * @return DTO с JWT токеном и информацией о ребёнке
     * @throws AuthenticationException если токен неверный
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod(value = "auth-login-child", logArgs = false)
    public LoginResponseDto loginChild(ChildLoginRequestDto request) {
        // Поиск ребёнка по loginToken
        Child child = childRepository.findByLoginToken(request.loginToken())
                .orElseThrow(() -> {
                    log.warn("Попытка входа с неверным loginToken: {}", request.loginToken());
                    return new AuthenticationException("Неверный токен входа");
                });

        // Генерация JWT токена для ребёнка
        String token = jwtTokenProvider.generateChildToken(
                child.getId(),
                child.getFirstName()
        );

        String displayName = child.getFirstName() + " " + child.getSurname();
        log.info("Успешная авторизация ребёнка: {}", displayName);

        return new LoginResponseDto(
                token,
                displayName,
                "CHILD"
        );
    }
}
