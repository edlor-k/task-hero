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
     * Начислить награду ребёнку с возможным ограничением EXP.
     */
    void grantReward(UUID childId, int exp, int coins, boolean capExp);

    /**
     * Начислить награду ребёнку за конкретное назначение задания — идемпотентно.
     * {@code sourceAssignmentId} используется user-service как ключ дедупликации:
     * повторный вызов с тем же ID (например, после ретрая упавшего запроса или
     * двойного клика) не приведёт к повторному начислению.
     *
     * @param childId            ID ребёнка
     * @param sourceAssignmentId ID назначения задания — источник награды
     * @param exp                количество EXP
     * @param coins              количество коинов
     * @param capExp             ограничить EXP максимумом текущего уровня
     */
    void grantReward(UUID childId, UUID sourceAssignmentId, int exp, int coins, boolean capExp);

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
