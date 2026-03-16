package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.aop.LogMethod;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.UnauthorizedException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.DifficultyTrajectory;
import ru.taskhero.userservice.dto.LevelInfoDto;
import ru.taskhero.userservice.dto.LevelRewardCreateRequest;
import ru.taskhero.userservice.dto.LevelRewardResponseDto;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.LevelReward;
import ru.taskhero.userservice.entity.ShopItem;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.LevelRewardRepository;
import ru.taskhero.userservice.repository.ShopItemRepository;
import ru.taskhero.userservice.service.LevelRewardService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Реализация сервиса наград за достижение уровней.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LevelRewardServiceImpl implements LevelRewardService {

    private final LevelRewardRepository levelRewardRepository;
    private final ChildRepository childRepository;
    private final ShopItemRepository shopItemRepository;

    private static final int BASE_EXP = 100;
    private static final double LEVEL_MULTIPLIER = 1.5;

    @Override
    @Transactional
    @LogMethod("level-reward-create")
    public List<LevelRewardResponseDto> createRewards(UUID childId, UUID parentId,
                                                       List<LevelRewardCreateRequest> rewards) {
        log.info("Создание {} наград за уровни для ребёнка {} родителем {}", rewards.size(), childId, parentId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребёнок с ID " + childId + " не найден"));

        if (!child.getParent().getId().equals(parentId)) {
            throw new UnauthorizedException("Вы не можете управлять наградами этого ребёнка");
        }

        List<LevelReward> created = new ArrayList<>();
        for (LevelRewardCreateRequest request : rewards) {
            if (levelRewardRepository.existsByChildIdAndLevel(childId, request.level())) {
                throw new ValidationException("Награда для уровня " + request.level() + " уже существует");
            }

            String shopItemTitle = null;
            if (request.shopItemId() != null) {
                ShopItem shopItem = shopItemRepository.findById(request.shopItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));
                shopItemTitle = shopItem.getTitle();
            }

            LevelReward reward = LevelReward.builder()
                    .child(child)
                    .level(request.level())
                    .title(request.title())
                    .description(request.description())
                    .shopItemId(request.shopItemId())
                    .build();

            reward = levelRewardRepository.save(reward);
            created.add(reward);
        }

        log.info("Создано {} наград за уровни для ребёнка {}", created.size(), childId);
        return created.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("level-reward-get-by-child")
    public List<LevelRewardResponseDto> getRewardsByChild(UUID childId) {
        return levelRewardRepository.findAllByChildIdOrderByLevel(childId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("level-reward-get-history")
    public List<LevelRewardResponseDto> getRewardHistory(UUID childId) {
        return levelRewardRepository.findAllByChildIdAndClaimedTrueOrderByLevel(childId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("level-reward-get-unseen")
    public List<LevelRewardResponseDto> getUnseenRewards(UUID childId) {
        return levelRewardRepository.findAllByChildIdAndClaimedTrueAndSeenFalse(childId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    @LogMethod("level-reward-mark-seen")
    public LevelRewardResponseDto markRewardSeen(UUID rewardId, UUID childId) {
        LevelReward reward = levelRewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Награда не найдена"));

        if (!reward.getChild().getId().equals(childId)) {
            throw new UnauthorizedException("Эта награда не принадлежит вам");
        }

        reward.setSeen(true);
        reward = levelRewardRepository.save(reward);
        log.info("Награда {} помечена как просмотренная ребёнком {}", rewardId, childId);
        return toDto(reward);
    }

    @Override
    @Transactional
    @LogMethod("level-reward-mark-issued")
    public LevelRewardResponseDto markRewardIssued(UUID rewardId, UUID parentId) {
        LevelReward reward = levelRewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Награда не найдена"));

        if (!reward.getChild().getParent().getId().equals(parentId)) {
            throw new UnauthorizedException("Вы не можете управлять этой наградой");
        }

        if (!reward.isClaimed()) {
            throw new ValidationException("Награда ещё не получена ребёнком");
        }

        reward.setIssued(true);
        reward.setIssuedAt(Instant.now());
        reward = levelRewardRepository.save(reward);
        log.info("Награда {} помечена как выданная родителем {}", rewardId, parentId);
        return toDto(reward);
    }

    @Override
    @Transactional
    @LogMethod("level-reward-check-and-claim")
    public void checkAndClaimRewards(UUID childId, int newLevel) {
        log.info("Проверка наград для ребёнка {} при достижении уровня {}", childId, newLevel);

        List<LevelReward> unclaimed = levelRewardRepository.findAllByChildIdAndClaimedFalseOrderByLevel(childId);
        int claimedCount = 0;

        for (LevelReward reward : unclaimed) {
            if (reward.getLevel() <= newLevel) {
                reward.setClaimed(true);
                reward.setClaimedAt(Instant.now());
                levelRewardRepository.save(reward);
                claimedCount++;
            }
        }

        if (claimedCount > 0) {
            log.info("Ребёнок {} получил {} наград при достижении уровня {}", childId, claimedCount, newLevel);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("level-reward-unfilled-count")
    public Map<String, Integer> getUnfilledLevelCount(UUID childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребёнок не найден"));

        int maxRewardedLevel = levelRewardRepository.findMaxLevelByChildId(childId);
        int currentLevel = child.getLevel();

        // Количество уровней впереди ребёнка, для которых есть награды
        int unfilledCount = Math.max(0, maxRewardedLevel - currentLevel);

        return Map.of("unfilledCount", unfilledCount, "maxRewardedLevel", maxRewardedLevel,
                "currentLevel", currentLevel);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("level-reward-level-info")
    public List<LevelInfoDto> getLevelInfo(UUID childId, int fromLevel, int toLevel) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребёнок не найден"));

        DifficultyTrajectory trajectory = child.getDifficultyTrajectory() != null
                ? child.getDifficultyTrajectory() : DifficultyTrajectory.NORMAL;

        double expMult = trajectory.getExpMultiplier();

        List<LevelInfoDto> levels = new ArrayList<>();
        for (int level = fromLevel; level <= toLevel; level++) {
            int expRequired = getExpForLevelIncremental(level);
            int totalExpForLevel = getExpForLevel(level);

            // Средние значения EXP за задание с учётом множителя траектории
            double avgEasyExp = 8 * expMult;
            double avgNormalExp = 10 * expMult;
            double avgHardExp = 15 * expMult;

            int approxEasy = expRequired > 0 ? (int) Math.ceil(expRequired / avgEasyExp) : 0;
            int approxNormal = expRequired > 0 ? (int) Math.ceil(expRequired / avgNormalExp) : 0;
            int approxHard = expRequired > 0 ? (int) Math.ceil(expRequired / avgHardExp) : 0;

            levels.add(new LevelInfoDto(level, expRequired, totalExpForLevel,
                    approxEasy, approxNormal, approxHard));
        }

        return levels;
    }

    @Override
    @Transactional
    @LogMethod("level-reward-delete")
    public void deleteReward(UUID rewardId, UUID parentId) {
        LevelReward reward = levelRewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Награда не найдена"));

        if (!reward.getChild().getParent().getId().equals(parentId)) {
            throw new UnauthorizedException("Вы не можете управлять этой наградой");
        }

        if (reward.isClaimed()) {
            throw new ValidationException("Нельзя удалить уже полученную награду");
        }

        levelRewardRepository.delete(reward);
        log.info("Награда {} удалена родителем {}", rewardId, parentId);
    }

    /**
     * Incremental EXP для конкретного уровня (сколько нужно от предыдущего до этого).
     */
    private int getExpForLevelIncremental(int level) {
        if (level <= 1) return 0;
        return (int) (BASE_EXP * Math.pow(LEVEL_MULTIPLIER, level - 2));
    }

    /**
     * Общий кумулятивный EXP для достижения уровня.
     */
    private int getExpForLevel(int level) {
        if (level <= 1) return 0;
        double totalExp = 0;
        for (int i = 2; i <= level; i++) {
            totalExp += BASE_EXP * Math.pow(LEVEL_MULTIPLIER, i - 2);
        }
        return (int) Math.round(totalExp);
    }

    /**
     * Конвертировать LevelReward в DTO.
     */
    private LevelRewardResponseDto toDto(LevelReward reward) {
        String shopItemTitle = null;
        if (reward.getShopItemId() != null) {
            shopItemTitle = shopItemRepository.findById(reward.getShopItemId())
                    .map(ShopItem::getTitle)
                    .orElse(null);
        }

        return new LevelRewardResponseDto(
                reward.getId(),
                reward.getChild().getId(),
                reward.getLevel(),
                reward.getTitle(),
                reward.getDescription(),
                reward.getShopItemId(),
                shopItemTitle,
                reward.isClaimed(),
                reward.getClaimedAt(),
                reward.isIssued(),
                reward.getIssuedAt(),
                reward.isSeen()
        );
    }
}
