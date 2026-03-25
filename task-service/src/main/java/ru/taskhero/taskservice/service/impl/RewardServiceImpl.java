package ru.taskhero.taskservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.taskhero.common.aop.LogMethod;
import ru.taskhero.taskservice.client.UserServiceClient;
import ru.taskhero.taskservice.dto.RewardRequest;
import ru.taskhero.taskservice.service.RewardService;

import java.util.UUID;

/**
 * Реализация сервиса для управления наградами.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final UserServiceClient userServiceClient;

    /**
     * Базовый EXP для первого уровня.
     */
    private static final int BASE_EXP = 100;

    /**
     * Множитель для расчета EXP следующего уровня.
     */
    private static final double LEVEL_MULTIPLIER = 1.5;

    @Override
    @LogMethod("reward-grant")
    public void grantReward(UUID childId, int exp, int coins) {
        grantReward(childId, exp, coins, false);
    }

    @Override
    @LogMethod("reward-grant")
    public void grantReward(UUID childId, int exp, int coins, boolean capExp) {
        log.info("Начисление награды ребёнку {}: {} EXP, {} коинов, capExp={}", childId, exp, coins, capExp);

        try {
            RewardRequest request = new RewardRequest(exp, coins, capExp);
            userServiceClient.addReward(childId, request);
            log.info("Награда успешно начислена ребёнку {}", childId);
        } catch (Exception e) {
            log.error("Ошибка при начислении награды ребёнку {}: {}", childId, e.getMessage());
            throw new RuntimeException("Не удалось начислить награду", e);
        }
    }

    @Override
    public int calculateLevel(int exp) {
        if (exp < BASE_EXP) {
            return 1;
        }

        // Формула: level = floor(sqrt(exp / BASE_EXP)) + 1
        // Прогрессивная формула, где каждый следующий уровень требует больше EXP
        int level = 1;
        int expRequired = 0;

        while (expRequired <= exp) {
            level++;
            expRequired = getExpForLevel(level);
        }

        return level - 1;
    }

    @Override
    public int getExpForNextLevel(int currentLevel) {
        return getExpForLevel(currentLevel + 1);
    }

    /**
     * Рассчитать общий EXP, необходимый для достижения указанного уровня.
     *
     * @param level целевой уровень
     * @return необходимый EXP
     */
    private int getExpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }

        // Прогрессивная формула: EXP(n) = BASE_EXP * (MULTIPLIER ^ (n-1))
        // Level 1: 0 EXP
        // Level 2: 100 EXP
        // Level 3: 250 EXP (100 + 150)
        // Level 4: 475 EXP (250 + 225)
        // ...

        double totalExp = 0;
        for (int i = 2; i <= level; i++) {
            totalExp += BASE_EXP * Math.pow(LEVEL_MULTIPLIER, i - 2);
        }

        return (int) Math.round(totalExp);
    }
}
