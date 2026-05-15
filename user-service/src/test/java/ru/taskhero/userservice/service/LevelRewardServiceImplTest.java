package ru.taskhero.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.UnauthorizedException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.userservice.dto.LevelRewardCreateRequest;
import ru.taskhero.userservice.dto.LevelRewardResponseDto;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.LevelReward;
import ru.taskhero.userservice.entity.Parent;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.LevelRewardRepository;
import ru.taskhero.userservice.repository.ShopItemRepository;
import ru.taskhero.userservice.service.impl.LevelRewardServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LevelRewardService Unit Tests")
class LevelRewardServiceImplTest {

    @Mock
    private LevelRewardRepository levelRewardRepository;

    @Mock
    private ChildRepository childRepository;

    @Mock
    private ShopItemRepository shopItemRepository;

    @InjectMocks
    private LevelRewardServiceImpl levelRewardService;

    private UUID parentId;
    private UUID childId;
    private UUID rewardId;
    private Parent parent;
    private Child child;
    private LevelReward levelReward;

    @BeforeEach
    void setUp() {
        parentId = UUID.randomUUID();
        childId = UUID.randomUUID();
        rewardId = UUID.randomUUID();

        parent = Parent.builder()
                .firstName("Иван")
                .surname("Петров")
                .build();

        child = Child.builder()
                .parent(parent)
                .firstName("Алиса")
                .exp(0)
                .coins(0)
                .level(1)
                .build();

        levelReward = LevelReward.builder()
                .child(child)
                .level(5)
                .title("Велосипед")
                .description("Новый велосипед за достижение 5 уровня")
                .build();
    }

    @Test
    @DisplayName("Должен создать награды за уровни для ребёнка")
    void shouldCreateLevelRewards() {
        // Given
        List<LevelRewardCreateRequest> requests = List.of(
                new LevelRewardCreateRequest(5, "Велосипед", "Новый велосипед", null),
                new LevelRewardCreateRequest(10, "Телефон", "Новый телефон", null)
        );
        when(childRepository.findByIdWithParent(childId)).thenReturn(Optional.of(child));
        when(levelRewardRepository.existsByChildIdAndLevel(eq(childId), anyInt())).thenReturn(false);
        when(levelRewardRepository.save(any(LevelReward.class))).thenReturn(levelReward);

        // Настраиваем parent ID через reflection
        try {
            java.lang.reflect.Field idField = ru.taskhero.common.model.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(parent, parentId);
        } catch (Exception ignored) {}

        // When
        List<LevelRewardResponseDto> result = levelRewardService.createRewards(childId, parentId, requests);

        // Then
        assertThat(result).hasSize(2);
        verify(levelRewardRepository, times(2)).save(any(LevelReward.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение если ребёнок не найден")
    void shouldThrowExceptionWhenChildNotFound() {
        // Given
        List<LevelRewardCreateRequest> requests = List.of(
                new LevelRewardCreateRequest(5, "Велосипед", null, null)
        );
        when(childRepository.findByIdWithParent(childId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> levelRewardService.createRewards(childId, parentId, requests))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("Должен выбросить исключение при попытке другого родителя управлять наградами")
    void shouldThrowExceptionForWrongParent() {
        // Given
        UUID wrongParentId = UUID.randomUUID();
        List<LevelRewardCreateRequest> requests = List.of(
                new LevelRewardCreateRequest(5, "Велосипед", null, null)
        );
        when(childRepository.findByIdWithParent(childId)).thenReturn(Optional.of(child));

        try {
            java.lang.reflect.Field idField = ru.taskhero.common.model.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(parent, parentId);
        } catch (Exception ignored) {}

        // When & Then
        assertThatThrownBy(() -> levelRewardService.createRewards(childId, wrongParentId, requests))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("не можете управлять");
    }

    @Test
    @DisplayName("Должен выбросить исключение при дублировании награды за уровень")
    void shouldThrowExceptionForDuplicateLevelReward() {
        // Given
        List<LevelRewardCreateRequest> requests = List.of(
                new LevelRewardCreateRequest(5, "Велосипед", null, null)
        );
        when(childRepository.findByIdWithParent(childId)).thenReturn(Optional.of(child));
        when(levelRewardRepository.existsByChildIdAndLevel(childId, 5)).thenReturn(true);

        try {
            java.lang.reflect.Field idField = ru.taskhero.common.model.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(parent, parentId);
        } catch (Exception ignored) {}

        // When & Then
        assertThatThrownBy(() -> levelRewardService.createRewards(childId, parentId, requests))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    @DisplayName("Должен автоматически начислять награды при повышении уровня")
    void shouldAutoClaimRewardsOnLevelUp() {
        // Given
        LevelReward reward3 = LevelReward.builder().child(child).level(3).title("Приз 3").build();
        LevelReward reward5 = LevelReward.builder().child(child).level(5).title("Приз 5").build();
        LevelReward reward7 = LevelReward.builder().child(child).level(7).title("Приз 7").build();

        when(levelRewardRepository.findAllByChildIdAndClaimedFalseOrderByLevel(childId))
                .thenReturn(List.of(reward3, reward5, reward7));
        when(levelRewardRepository.save(any(LevelReward.class))).thenAnswer(inv -> inv.getArgument(0));

        // When — ребёнок достиг уровня 5
        levelRewardService.checkAndClaimRewards(childId, 5);

        // Then — только уровни 3 и 5 должны быть начислены
        verify(levelRewardRepository, times(2)).save(any(LevelReward.class));
        assertThat(reward3.isClaimed()).isTrue();
        assertThat(reward5.isClaimed()).isTrue();
        assertThat(reward7.isClaimed()).isFalse();
    }

    @Test
    @DisplayName("Должен вернуть список наград ребёнка")
    void shouldGetRewardsByChild() {
        // Given
        when(levelRewardRepository.findAllByChildIdOrderByLevel(childId))
                .thenReturn(List.of(levelReward));

        // When
        List<LevelRewardResponseDto> result = levelRewardService.getRewardsByChild(childId);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Должен пометить награду как просмотренную")
    void shouldMarkRewardAsSeen() {
        // Given
        levelReward.setClaimed(true);
        levelReward.setSeen(false);

        try {
            java.lang.reflect.Field idField = ru.taskhero.common.model.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(child, childId);
        } catch (Exception ignored) {}

        when(levelRewardRepository.findByIdWithChild(rewardId)).thenReturn(Optional.of(levelReward));
        when(levelRewardRepository.save(any(LevelReward.class))).thenReturn(levelReward);

        // When
        LevelRewardResponseDto result = levelRewardService.markRewardSeen(rewardId, childId);

        // Then
        assertThat(result).isNotNull();
        assertThat(levelReward.isSeen()).isTrue();
        verify(levelRewardRepository).save(levelReward);
    }

    @Test
    @DisplayName("Должен выбросить исключение при попытке удалить уже начисленную награду")
    void shouldThrowExceptionWhenDeletingClaimedReward() {
        // Given
        levelReward.setClaimed(true);

        try {
            java.lang.reflect.Field idField = ru.taskhero.common.model.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(parent, parentId);
        } catch (Exception ignored) {}

        when(levelRewardRepository.findByIdWithChildAndParent(rewardId)).thenReturn(Optional.of(levelReward));

        // When & Then
        assertThatThrownBy(() -> levelRewardService.deleteReward(rewardId, parentId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Нельзя удалить");
    }
}
