package ru.taskhero.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.ParentResponseDto;
import ru.taskhero.userservice.dto.UpdateParentRequest;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.Parent;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.dto.ParentDetailDto;
import ru.taskhero.userservice.mapper.ChildMapper;
import ru.taskhero.userservice.mapper.ParentMapper;
import ru.taskhero.userservice.mapper.UserMapper;
import ru.taskhero.userservice.repository.ParentRepository;
import ru.taskhero.userservice.repository.UserRepository;
import ru.taskhero.userservice.service.impl.ParentServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParentService Unit Tests")
class ParentServiceImplTest {

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParentMapper parentMapper;

    @Mock
    private ChildMapper childMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ParentServiceImpl parentService;

    private UUID userId;
    private UUID parentId;
    private User user;
    private Parent parent;
    private ParentResponseDto parentDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        parentId = UUID.randomUUID();

        user = User.builder()
                .email("parent@example.com")
                .role(Role.PARENT)
                .active(true)
                .build();

        parent = Parent.builder()
                .user(user)
                .firstName("Иван")
                .surname("Петров")
                .children(List.of())
                .build();

        parentDto = new ParentResponseDto(parentId, userId, "Иван", "Петров", List.of());
    }

    @Test
    @DisplayName("Должен создать профиль родителя для пользователя")
    void shouldCreateParentForUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(parentRepository.save(any(Parent.class))).thenReturn(parent);
        when(parentMapper.toParentResponseDto(parent)).thenReturn(parentDto);

        // When
        ParentResponseDto result = parentService.createForUser(userId, "Иван", "Петров");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Иван");
        assertThat(result.surname()).isEqualTo("Петров");
        verify(parentRepository).save(any(Parent.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при создании профиля для несуществующего пользователя")
    void shouldThrowExceptionWhenUserNotFoundForParent() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> parentService.createForUser(userId, "Иван", "Петров"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("Должен вернуть профиль родителя по ID пользователя")
    void shouldGetParentByUserId() {
        // Given
        when(parentRepository.findByUserId(userId)).thenReturn(Optional.of(parent));
        when(parentMapper.toParentResponseDto(parent)).thenReturn(parentDto);

        // When
        ParentResponseDto result = parentService.getByUserId(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Иван");
    }

    @Test
    @DisplayName("Должен выбросить исключение если профиль родителя не найден")
    void shouldThrowExceptionWhenParentNotFoundByUserId() {
        // Given
        when(parentRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> parentService.getByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("Должен обновить профиль родителя (по ID родителя)")
    void shouldUpdateParent() {
        // Given
        UpdateParentRequest request = new UpdateParentRequest("Алексей", "Смирнов");
        when(parentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(parentRepository.save(parent)).thenReturn(parent);
        when(parentMapper.toParentResponseDto(parent)).thenReturn(
                new ParentResponseDto(parentId, userId, "Алексей", "Смирнов", List.of())
        );

        // When
        ParentResponseDto result = parentService.updateParent(parentId, request);

        // Then
        assertThat(result.firstName()).isEqualTo("Алексей");
        assertThat(result.surname()).isEqualTo("Смирнов");
        verify(parentRepository).save(parent);
    }

    @Test
    @DisplayName("Должен не менять поля при обновлении с пустыми значениями")
    void shouldNotUpdateBlankFields() {
        // Given
        UpdateParentRequest request = new UpdateParentRequest("", null);
        when(parentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(parentRepository.save(parent)).thenReturn(parent);
        when(parentMapper.toParentResponseDto(parent)).thenReturn(parentDto);

        // When
        ParentResponseDto result = parentService.updateParent(parentId, request);

        // Then
        assertThat(result.firstName()).isEqualTo("Иван");
        assertThat(result.surname()).isEqualTo("Петров");
    }

    @Test
    @DisplayName("Должен выбросить исключение при обновлении несуществующего родителя")
    void shouldThrowExceptionWhenParentNotFoundForUpdate() {
        // Given
        UpdateParentRequest request = new UpdateParentRequest("Новое", "Имя");
        when(parentRepository.findById(parentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> parentService.updateParent(parentId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("Должен обновить профиль текущего родителя (по userId)")
    void shouldUpdateCurrentParent() {
        // Given
        UpdateParentRequest request = new UpdateParentRequest("Пётр", null);
        when(parentRepository.findByUserId(userId)).thenReturn(Optional.of(parent));
        when(parentRepository.save(parent)).thenReturn(parent);
        when(parentMapper.toParentResponseDto(parent)).thenReturn(
                new ParentResponseDto(parentId, userId, "Пётр", "Петров", List.of())
        );

        // When
        ParentResponseDto result = parentService.updateCurrentParent(userId, request);

        // Then
        assertThat(result.firstName()).isEqualTo("Пётр");
        verify(parentRepository).save(parent);
    }

    @Test
    @DisplayName("Должен выбросить исключение при обновлении профиля текущего родителя если не найден")
    void shouldThrowExceptionWhenCurrentParentNotFound() {
        // Given
        UpdateParentRequest request = new UpdateParentRequest("Пётр", null);
        when(parentRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> parentService.updateCurrentParent(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("Должен вернуть детальную информацию о родителе")
    void shouldGetParentDetailById() {
        // Given
        Child child = Child.builder()
                .firstName("Алиса")
                .surname("Петрова")
                .build();
        parent.setChildren(List.of(child));

        when(parentRepository.findByIdWithChildrenAndUser(parentId)).thenReturn(Optional.of(parent));

        // When
        var result = parentService.getDetailById(parentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Иван");
        assertThat(result.children()).hasSize(1);
    }

    @Test
    @DisplayName("Должен выбросить исключение при поиске несуществующего родителя по ID")
    void shouldThrowExceptionWhenDetailNotFound() {
        // Given
        when(parentRepository.findByIdWithChildrenAndUser(parentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> parentService.getDetailById(parentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }
}
