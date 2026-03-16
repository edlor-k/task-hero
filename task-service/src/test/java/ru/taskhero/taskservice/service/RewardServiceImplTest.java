package ru.taskhero.taskservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taskhero.taskservice.client.UserServiceClient;
import ru.taskhero.taskservice.dto.ChildResponseDto;
import ru.taskhero.taskservice.dto.RewardRequest;
import ru.taskhero.taskservice.service.impl.RewardServiceImpl;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardService Unit Tests")
class RewardServiceImplTest {

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private RewardServiceImpl rewardService;

    @Test
    @DisplayName("Должен начислить награду ребёнку")
    void shouldGrantReward() {
        // Given
        UUID childId = UUID.randomUUID();
        int exp = 25;
        int coins = 10;

        ChildResponseDto mockResponse = new ChildResponseDto(
                childId, "Вася", "Иванов", "token123",
                100, 50, 2, null, Instant.now(), null
        );

        when(userServiceClient.addReward(eq(childId), any(RewardRequest.class)))
                .thenReturn(mockResponse);

        // When
        rewardService.grantReward(childId, exp, coins);

        // Then
        verify(userServiceClient).addReward(eq(childId), any(RewardRequest.class));
    }

    @Test
    @DisplayName("Должен рассчитать уровень 1 для EXP < 100")
    void shouldCalculateLevelOneForLowExp() {
        // When
        int level = rewardService.calculateLevel(50);

        // Then
        assertThat(level).isEqualTo(1);
    }

    @Test
    @DisplayName("Должен рассчитать уровень 2 для EXP >= 100")
    void shouldCalculateLevelTwoForExp100() {
        // When
        int level = rewardService.calculateLevel(100);

        // Then
        assertThat(level).isEqualTo(2);
    }

    @Test
    @DisplayName("Должен рассчитать уровень 3 для достаточного EXP")
    void shouldCalculateLevelThreeForHigherExp() {
        // When
        int level = rewardService.calculateLevel(250);

        // Then
        assertThat(level).isEqualTo(3);
    }

    @Test
    @DisplayName("Должен вернуть EXP для следующего уровня")
    void shouldGetExpForNextLevel() {
        // When
        int expForLevel2 = rewardService.getExpForNextLevel(1);

        // Then
        assertThat(expForLevel2).isGreaterThan(0);
    }
}
