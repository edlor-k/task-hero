package ru.taskhero.taskservice.service;

import java.util.UUID;

/**
 * Сервис для управления наградами.
 */
public interface RewardService {

    /**
     * Начислить награду ребёнку.
     *
     * @param childId ID ребёнка
     * @param exp     количество EXP
     * @param coins   количество коинов
     */
    void grantReward(UUID childId, int exp, int coins);

    /**
     * Рассчитать уровень по количеству EXP.
     *
     * @param exp количество EXP
     * @return уровень
     */
    int calculateLevel(int exp);

    /**
     * Рассчитать EXP, необходимый для следующего уровня.
     *
     * @param currentLevel текущий уровень
     * @return необходимый EXP
     */
    int getExpForNextLevel(int currentLevel);
}
