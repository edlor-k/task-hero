package ru.taskhero.userservice.service;

import ru.taskhero.userservice.dto.LevelInfoDto;
import ru.taskhero.userservice.dto.LevelRewardCreateRequest;
import ru.taskhero.userservice.dto.LevelRewardResponseDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сервис для управления наградами за достижение уровней.
 */
public interface LevelRewardService {

    /**
     * Создать награды за уровни (bulk).
     */
    List<LevelRewardResponseDto> createRewards(UUID childId, UUID parentId, List<LevelRewardCreateRequest> rewards);

    /**
     * Получить все награды ребёнка.
     */
    List<LevelRewardResponseDto> getRewardsByChild(UUID childId);

    /**
     * Получить историю полученных наград (claimed=true).
     */
    List<LevelRewardResponseDto> getRewardHistory(UUID childId);

    /**
     * Получить непросмотренные награды ребёнка (claimed=true AND seen=false).
     */
    List<LevelRewardResponseDto> getUnseenRewards(UUID childId);

    /**
     * Пометить награду как просмотренную ребёнком.
     */
    LevelRewardResponseDto markRewardSeen(UUID rewardId, UUID childId);

    /**
     * Отметить награду как выданную родителем.
     */
    LevelRewardResponseDto markRewardIssued(UUID rewardId, UUID parentId);

    /**
     * Проверить и пометить награды как claimed при повышении уровня.
     */
    void checkAndClaimRewards(UUID childId, int newLevel);

    /**
     * Получить количество уровней впереди ребёнка без награды.
     */
    Map<String, Integer> getUnfilledLevelCount(UUID childId);

    /**
     * Получить информацию об уровнях (XP, примерное кол-во заданий).
     */
    List<LevelInfoDto> getLevelInfo(UUID childId, int fromLevel, int toLevel);

    /**
     * Удалить незаклейменную награду.
     */
    void deleteReward(UUID rewardId, UUID parentId);
}
