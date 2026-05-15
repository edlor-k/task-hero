package ru.taskhero.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.ParentResponseDto;
import ru.taskhero.userservice.dto.RegisterResponseDto;
import ru.taskhero.userservice.dto.UserRegisterRequest;
import ru.taskhero.userservice.dto.UserResponseDto;
import ru.taskhero.userservice.entity.AuditAction;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.mapper.UserMapper;
import ru.taskhero.userservice.repository.UserRepository;
import ru.taskhero.userservice.service.AuditService;
import ru.taskhero.userservice.service.ParentService;
import ru.taskhero.userservice.service.impl.UserServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParentService parentService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;
    private UserResponseDto userDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .email("parent@example.com")
                .password("hashed_password")
                .role(Role.PARENT)
                .active(true)
                .build();

        userDto = new UserResponseDto(userId, "parent@example.com", Role.PARENT, true);
    }

    @Test
    @DisplayName("Должен зарегистрировать нового родителя")
    void shouldRegisterNewParent() {
        // Given
        UserRegisterRequest request = new UserRegisterRequest(
                "parent@example.com", "password123", "Иван", "Петров"
        );
        UUID parentProfileId = UUID.randomUUID();
        ParentResponseDto parentDto = new ParentResponseDto(parentProfileId, userId, "Иван", "Петров", List.of());

        when(userRepository.existsByEmail("parent@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(parentService.createForUser(any(), eq("Иван"), eq("Петров"))).thenReturn(parentDto);
        doNothing().when(auditService).log(any(), anyString(), any(AuditAction.class), anyString(), any(), anyString());

        // When
        RegisterResponseDto result = userService.register(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("parent@example.com");
        assertThat(result.role()).isEqualTo(Role.PARENT);
        assertThat(result.isActive()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(parentService).createForUser(any(), eq("Иван"), eq("Петров"));
    }

    @Test
    @DisplayName("Должен выбросить исключение при регистрации с существующим email")
    void shouldThrowExceptionForDuplicateEmail() {
        // Given
        UserRegisterRequest request = new UserRegisterRequest(
                "existing@example.com", "password123", "Иван", "Петров"
        );
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    @DisplayName("Должен вернуть пользователя по ID")
    void shouldGetUserById() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        // When
        UserResponseDto result = userService.getById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("parent@example.com");
    }

    @Test
    @DisplayName("Должен выбросить исключение при поиске несуществующего пользователя")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("Должен вернуть true при проверке существующего email")
    void shouldReturnTrueForExistingEmail() {
        // Given
        when(userRepository.existsByEmail("parent@example.com")).thenReturn(true);

        // When
        boolean result = userService.existsByEmail("parent@example.com");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Должен получить всех пользователей без фильтров")
    void shouldGetAllUsersWithoutFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // When
        Page<UserResponseDto> result = userService.getAllUsers(pageable, null, null);

        // Then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Должен получить пользователей по роли")
    void shouldGetUsersByRole() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findByRole(Role.PARENT, pageable)).thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // When
        Page<UserResponseDto> result = userService.getAllUsers(pageable, Role.PARENT, null);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByRole(Role.PARENT, pageable);
    }

    @Test
    @DisplayName("Должен получить пользователей по активности")
    void shouldGetUsersByActive() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findByActive(true, pageable)).thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // When
        Page<UserResponseDto> result = userService.getAllUsers(pageable, null, true);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByActive(true, pageable);
    }

    @Test
    @DisplayName("Должен получить пользователей по роли и активности")
    void shouldGetUsersByRoleAndActive() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findByRoleAndActive(Role.PARENT, true, pageable)).thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // When
        Page<UserResponseDto> result = userService.getAllUsers(pageable, Role.PARENT, true);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByRoleAndActive(Role.PARENT, true, pageable);
    }

    @Test
    @DisplayName("Должен переключить активность пользователя (активный → неактивный)")
    void shouldToggleActiveToInactive() {
        // Given
        user.setActive(true);
        UserResponseDto inactiveDto = new UserResponseDto(userId, "parent@example.com", Role.PARENT, false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(inactiveDto);

        // When
        UserResponseDto result = userService.toggleActive(userId);

        // Then
        assertThat(result.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Должен переключить активность пользователя (неактивный → активный)")
    void shouldToggleActiveToActive() {
        // Given
        user.setActive(false);
        UserResponseDto activeDto = new UserResponseDto(userId, "parent@example.com", Role.PARENT, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(activeDto);

        // When
        UserResponseDto result = userService.toggleActive(userId);

        // Then
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("Должен обновить роль пользователя")
    void shouldUpdateUserRole() {
        // Given
        user.setRole(Role.PARENT);
        UserResponseDto adminDto = new UserResponseDto(userId, "parent@example.com", Role.ADMIN, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(adminDto);

        // When
        UserResponseDto result = userService.updateRole(userId, Role.ADMIN);

        // Then
        assertThat(result.role()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Должен выбросить исключение при обновлении на ту же роль")
    void shouldThrowExceptionWhenRoleUnchanged() {
        // Given
        user.setRole(Role.PARENT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.updateRole(userId, Role.PARENT))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("уже имеет роль");
    }

    @Test
    @DisplayName("Должен деактивировать (удалить) пользователя")
    void shouldDeleteActiveUser() {
        // Given
        user.setActive(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).save(user);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Должен выбросить исключение при удалении уже неактивного пользователя")
    void shouldThrowExceptionWhenDeletingInactiveUser() {
        // Given
        user.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("уже неактивен");
    }

    @Test
    @DisplayName("Должен найти пользователей по email")
    void shouldSearchByEmail() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.searchByEmail("parent", pageable)).thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // When
        Page<UserResponseDto> result = userService.searchByEmail("parent", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Должен сбросить пароль пользователя")
    void shouldResetPassword() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("hashed_new");
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.resetPassword(userId, "newpassword");

        // Then
        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("hashed_new");
    }
}
