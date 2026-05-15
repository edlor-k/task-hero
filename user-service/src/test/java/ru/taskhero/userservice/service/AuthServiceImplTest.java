package ru.taskhero.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.taskhero.common.exception.AuthenticationException;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.ChildLoginRequestDto;
import ru.taskhero.userservice.dto.LoginResponseDto;
import ru.taskhero.userservice.dto.UserLoginRequestDto;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.UserRepository;
import ru.taskhero.userservice.security.JwtTokenProvider;
import ru.taskhero.userservice.service.impl.AuthServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChildRepository childRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private User user;
    private Child child;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .email("parent@example.com")
                .password("hashed_password")
                .role(Role.PARENT)
                .active(true)
                .build();

        child = Child.builder()
                .firstName("Алиса")
                .surname("Иванова")
                .loginToken("VALID-TOKEN-123")
                .exp(0)
                .coins(0)
                .level(1)
                .build();
    }

    @Test
    @DisplayName("Должен успешно авторизовать родителя")
    void shouldLoginParentSuccessfully() {
        // Given
        UserLoginRequestDto request = new UserLoginRequestDto("parent@example.com", "password123");
        when(userRepository.findByEmail("parent@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(user.getId(), user.getEmail(), "PARENT"))
                .thenReturn("jwt-token");

        // When
        LoginResponseDto result = authService.loginUser(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.displayName()).isEqualTo("parent@example.com");
        assertThat(result.role()).isEqualTo("PARENT");
    }

    @Test
    @DisplayName("Должен выбросить исключение при несуществующем email")
    void shouldThrowExceptionForUnknownEmail() {
        // Given
        UserLoginRequestDto request = new UserLoginRequestDto("unknown@example.com", "password");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Неверный email или пароль");
    }

    @Test
    @DisplayName("Должен выбросить исключение при неверном пароле")
    void shouldThrowExceptionForWrongPassword() {
        // Given
        UserLoginRequestDto request = new UserLoginRequestDto("parent@example.com", "wrong_password");
        when(userRepository.findByEmail("parent@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Неверный email или пароль");
    }

    @Test
    @DisplayName("Должен выбросить исключение при попытке входа заблокированного пользователя")
    void shouldThrowExceptionForBlockedUser() {
        // Given
        user.setActive(false);
        UserLoginRequestDto request = new UserLoginRequestDto("parent@example.com", "password123");
        when(userRepository.findByEmail("parent@example.com")).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("заблокирована");
    }

    @Test
    @DisplayName("Должен успешно авторизовать ребёнка по loginToken")
    void shouldLoginChildByToken() {
        // Given
        ChildLoginRequestDto request = new ChildLoginRequestDto("VALID-TOKEN-123");
        when(childRepository.findByLoginToken("VALID-TOKEN-123")).thenReturn(Optional.of(child));
        when(jwtTokenProvider.generateChildToken(child.getId(), "Алиса")).thenReturn("child-jwt-token");

        // When
        LoginResponseDto result = authService.loginChild(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("child-jwt-token");
        assertThat(result.role()).isEqualTo("CHILD");
    }

    @Test
    @DisplayName("Должен выбросить исключение при неверном loginToken ребёнка")
    void shouldThrowExceptionForInvalidChildToken() {
        // Given
        ChildLoginRequestDto request = new ChildLoginRequestDto("INVALID-TOKEN");
        when(childRepository.findByLoginToken("INVALID-TOKEN")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.loginChild(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Неверный токен входа");
    }
}
