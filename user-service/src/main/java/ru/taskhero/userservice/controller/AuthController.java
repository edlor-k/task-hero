package ru.taskhero.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.taskhero.common.exception.ExceptionBody;
import ru.taskhero.userservice.dto.ChildLoginRequestDto;
import ru.taskhero.userservice.dto.LoginResponseDto;
import ru.taskhero.userservice.dto.RegisterResponseDto;
import ru.taskhero.userservice.dto.SocialLoginRequestDto;
import ru.taskhero.userservice.dto.UserLoginRequestDto;
import ru.taskhero.userservice.dto.UserRegisterRequest;
import ru.taskhero.userservice.service.AuthService;
import ru.taskhero.userservice.service.UserService;

/**
 * Контроллер для аутентификации и регистрации пользователей.
 * Все endpoints публичные (не требуют JWT).
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints для регистрации и входа пользователей")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Регистрация нового родителя.
     *
     * @param request данные для регистрации
     * @return DTO зарегистрированного пользователя и профиля родителя
     */
    @PostMapping("/register")
    @Operation(
            summary = "Регистрация нового родителя",
            description = "Создает нового пользователя с ролью PARENT и связанный профиль родителя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(schema = @Schema(implementation = RegisterResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные или email уже существует",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("Запрос на регистрацию нового пользователя: {}", request.email());
        RegisterResponseDto response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Вход родителя или администратора по email и паролю.
     *
     * @param request данные для входа
     * @return JWT токен и информация о пользователе
     */
    @PostMapping("/login")
    @Operation(
            summary = "Вход родителя или администратора",
            description = "Аутентификация по email и паролю, возвращает JWT токен"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная аутентификация",
                    content = @Content(schema = @Schema(implementation = LoginResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверный email или пароль",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody UserLoginRequestDto request) {
        log.info("Попытка входа пользователя: {}", request.email());
        LoginResponseDto response = authService.loginUser(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Вход ребенка по уникальному токену.
     *
     * @param request данные для входа (loginToken)
     * @return JWT токен и информация о ребенке
     */
    @PostMapping("/social-login")
    @Operation(
            summary = "Вход через Yandex SSO",
            description = "Находит пользователя по email или создаёт нового родителя. Вызывается только из app-сервиса."
    )
    public ResponseEntity<LoginResponseDto> socialLogin(@Valid @RequestBody SocialLoginRequestDto request) {
        log.info("SSO-вход: {}", request.email());
        LoginResponseDto response = authService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/child")
    @Operation(
            summary = "Вход ребенка по токену",
            description = "Аутентификация ребенка по уникальному loginToken, возвращает JWT токен"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная аутентификация",
                    content = @Content(schema = @Schema(implementation = LoginResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверный токен входа",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<LoginResponseDto> loginChild(@Valid @RequestBody ChildLoginRequestDto request) {
        log.info("Попытка входа ребенка по токену");
        LoginResponseDto response = authService.loginChild(request);
        return ResponseEntity.ok(response);
    }
}
