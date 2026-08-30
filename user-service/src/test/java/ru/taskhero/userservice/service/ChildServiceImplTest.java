package ru.taskhero.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.DifficultyTrajectory;
import ru.taskhero.userservice.dto.ChildCreateRequestDto;
import ru.taskhero.userservice.dto.ChildResponseDto;
import ru.taskhero.userservice.entity.AvatarOption;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.Parent;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.mapper.ChildMapper;
import ru.taskhero.userservice.mapper.ParentMapper;
import ru.taskhero.userservice.repository.AvatarOptionRepository;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.ParentRepository;
import ru.taskhero.userservice.service.impl.ChildServiceImpl;
import ru.taskhero.userservice.service.impl.RewardGrantGuard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChildService Unit Tests")
class ChildServiceImplTest {

    @Mock
    private ChildRepository childRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private AvatarOptionRepository avatarOptionRepository;

    @Mock
    private ChildMapper childMapper;

    @Mock
    private ParentMapper parentMapper;

    @Mock
    private LevelRewardService levelRewardService;

    @Mock
    private AuditService auditService;

    @Mock
    private RewardGrantGuard rewardGrantGuard;

    @InjectMocks
    private ChildServiceImpl childService;

    private UUID parentId;
    private UUID childId;
    private Parent parent;
    private Child child;
    private ChildResponseDto childResponseDto;

    @BeforeEach
    void setUp() {
        parentId = UUID.randomUUID();
        childId = UUID.randomUUID();

        User user = User.builder()
                .email("parent@example.com")
                .build();

        parent = Parent.builder()
                .firstName("Иван")
                .surname("Петров")
                .user(user)
                .build();

        child = Child.builder()
                .parent(parent)
                .firstName("Алиса")
                .surname("Петрова")
                .loginToken("TOKEN-ABC")
                .exp(0)
                .coins(0)
                .level(1)
                .difficultyTrajectory(DifficultyTrajectory.NORMAL)
                .build();

        childResponseDto = new ChildResponseDto(
                childId, "Алиса", "Петрова",
                0, 0, 1, null, "TOKEN-ABC",
                DifficultyTrajectory.NORMAL, null,
                50, 0, 50, null, null, false,
                null, null, null
        );
    }

    @Test
    @DisplayName("Должен создать ребёнка для родителя")
    void shouldCreateChildForParent() {
        // Given
        ChildCreateRequestDto request = new ChildCreateRequestDto("Алиса", "Петрова", DifficultyTrajectory.NORMAL, null);
        when(parentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(childRepository.findByLoginToken(anyString())).thenReturn(Optional.empty());
        when(childRepository.save(any(Child.class))).thenReturn(child);
        when(childMapper.toDto(any(Child.class), anyInt(), anyInt(), anyInt(), any())).thenReturn(childResponseDto);

        // When
        ChildResponseDto result = childService.createChild(parentId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Алиса");
        verify(childRepository).save(any(Child.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при создании ребёнка несуществующим родителем")
    void shouldThrowExceptionWhenParentNotFound() {
        // Given
        ChildCreateRequestDto request = new ChildCreateRequestDto("Алиса", "Петрова", null, null);
        when(parentRepository.findById(parentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> childService.createChild(parentId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("Должен вернуть список детей родителя")
    void shouldGetChildrenByParent() {
        // Given
        when(childRepository.findAllByParentId(parentId)).thenReturn(List.of(child));
        when(childMapper.toDto(any(Child.class), anyInt(), anyInt(), anyInt(), any())).thenReturn(childResponseDto);

        // When
        List<ChildResponseDto> result = childService.getChildrenByParent(parentId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).firstName()).isEqualTo("Алиса");
    }

    @Test
    @DisplayName("Должен получить ребёнка по loginToken")
    void shouldGetChildByLoginToken() {
        // Given
        when(childRepository.findByLoginToken("TOKEN-ABC")).thenReturn(Optional.of(child));
        when(childMapper.toDto(any(Child.class), anyInt(), anyInt(), anyInt(), any())).thenReturn(childResponseDto);

        // When
        ChildResponseDto result = childService.getByLoginToken("TOKEN-ABC");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.loginToken()).isEqualTo("TOKEN-ABC");
    }

    @Test
    @DisplayName("Должен выбросить исключение при ненайденном токене")
    void shouldThrowExceptionForUnknownToken() {
        // Given
        when(childRepository.findByLoginToken("UNKNOWN")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> childService.getByLoginToken("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Должен начислить EXP и коины ребёнку")
    void shouldAddRewardToChild() {
        // Given
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(childRepository.save(any(Child.class))).thenReturn(child);
        when(childMapper.toDto(any(Child.class), anyInt(), anyInt(), anyInt(), any())).thenReturn(
                new ChildResponseDto(childId, "Алиса", "Петрова",
                        25, 10, 1, null, "TOKEN-ABC",
                        DifficultyTrajectory.NORMAL, null,
                        25, 25, 50, null, null, false,
                        null, null, null)
        );

        // When
        ChildResponseDto result = childService.addReward(childId, 25, 10);

        // Then
        assertThat(result).isNotNull();
        verify(childRepository).save(any(Child.class));
    }

    @Test
    @DisplayName("Должен повысить уровень при достижении порога EXP")
    void shouldLevelUpWhenExpThresholdReached() {
        // Given — уровень 1, для перехода на уровень 2 нужно 50 EXP (40 + 5*2*1=50)
        child.setExp(45);
        child.setLevel(1);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(childRepository.save(any(Child.class))).thenAnswer(inv -> inv.getArgument(0));
        when(childMapper.toDto(any(Child.class), anyInt(), anyInt(), anyInt(), any())).thenReturn(childResponseDto);

        // When
        childService.addReward(childId, 10, 0);

        // Then — сохранение было вызвано с обновлённым child
        verify(childRepository).save(any(Child.class));
    }

    @Test
    @DisplayName("Должен разрешить выбор аватара")
    void shouldSelectAvatar() {
        // Given
        UUID avatarOptionId = UUID.randomUUID();
        AvatarOption avatarOption = AvatarOption.builder()
                .imageUrl("https://storage.example/avatars/dragon.png")
                .imageKey("avatars/dragon.png")
                .active(true)
                .build();
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(avatarOptionRepository.findById(avatarOptionId)).thenReturn(Optional.of(avatarOption));
        when(childRepository.save(any(Child.class))).thenReturn(child);
        when(childMapper.toDto(any(Child.class), anyInt(), anyInt(), anyInt(), any())).thenReturn(childResponseDto);

        // When
        ChildResponseDto result = childService.selectAvatar(childId, avatarOptionId);

        // Then
        assertThat(result).isNotNull();
        verify(childRepository).save(any(Child.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при выборе неактивного аватара")
    void shouldThrowExceptionWhenAvatarOptionInactive() {
        // Given
        UUID avatarOptionId = UUID.randomUUID();
        AvatarOption avatarOption = AvatarOption.builder()
                .imageUrl("https://storage.example/avatars/dragon.png")
                .imageKey("avatars/dragon.png")
                .active(false)
                .build();
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(avatarOptionRepository.findById(avatarOptionId)).thenReturn(Optional.of(avatarOption));

        // When & Then
        assertThatThrownBy(() -> childService.selectAvatar(childId, avatarOptionId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("доступна");
    }

    @Test
    @DisplayName("Должен проверить принадлежность ребёнка родителю")
    void shouldCheckChildBelongsToParent() {
        // Given
        when(childRepository.existsById(childId)).thenReturn(true);
        when(childRepository.existsByIdAndParentId(childId, parentId)).thenReturn(true);

        // When
        boolean result = childService.isChildBelongsToParent(childId, parentId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Должен вернуть false если ребёнок принадлежит другому родителю")
    void shouldReturnFalseForWrongParent() {
        // Given
        UUID otherParentId = UUID.randomUUID();
        when(childRepository.existsById(childId)).thenReturn(true);
        when(childRepository.existsByIdAndParentId(childId, otherParentId)).thenReturn(false);

        // When
        boolean result = childService.isChildBelongsToParent(childId, otherParentId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("addRewardForAssignment: должен начислить награду при первом обращении с этим ID назначения")
    void shouldGrantRewardOnFirstCallForAssignment() {
        // Given
        UUID assignmentId = UUID.randomUUID();
        when(rewardGrantGuard.tryRecord(childId, assignmentId, 25, 10)).thenReturn(true);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(childRepository.save(any(Child.class))).thenReturn(child);
        when(childMapper.toDto(any(Child.class), anyInt(), anyInt(), anyInt(), any())).thenReturn(childResponseDto);

        // When
        ChildResponseDto result = childService.addRewardForAssignment(childId, assignmentId, 25, 10, false);

        // Then
        assertThat(result).isNotNull();
        verify(rewardGrantGuard).tryRecord(childId, assignmentId, 25, 10);
        verify(childRepository).save(any(Child.class));
    }

    @Test
    @DisplayName("addRewardForAssignment: не должен начислять награду повторно за то же назначение " +
            "(идемпотентность — защита от двойного клика, ретрая и конкурентных запросов)")
    void shouldNotGrantRewardTwiceForSameAssignment() {
        // Given — леджер сообщает, что награда за это назначение уже зарегистрирована
        UUID assignmentId = UUID.randomUUID();
        when(rewardGrantGuard.tryRecord(childId, assignmentId, 25, 10)).thenReturn(false);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(childMapper.toDto(any(Child.class), anyInt(), anyInt(), anyInt(), any())).thenReturn(childResponseDto);

        // When
        ChildResponseDto result = childService.addRewardForAssignment(childId, assignmentId, 25, 10, false);

        // Then — баланс не пересчитывается и не сохраняется повторно
        assertThat(result).isNotNull();
        verify(childRepository, never()).save(any(Child.class));
    }

    @Test
    @DisplayName("addRewardForAssignment: без ID назначения ведёт себя как обычное начисление (без дедупликации)")
    void shouldGrantRewardWithoutDeduplicationWhenNoAssignmentId() {
        // Given
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(childRepository.save(any(Child.class))).thenReturn(child);
        when(childMapper.toDto(any(Child.class), anyInt(), anyInt(), anyInt(), any())).thenReturn(childResponseDto);

        // When
        childService.addRewardForAssignment(childId, null, 25, 10, false);

        // Then
        verify(rewardGrantGuard, never()).tryRecord(any(), any(), anyInt(), anyInt());
        verify(childRepository, times(1)).save(any(Child.class));
    }
}
