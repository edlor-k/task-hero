package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.aop.LogMethod;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.CharacterType;
import ru.taskhero.common.model.enums.DifficultyTrajectory;
import ru.taskhero.userservice.dto.ChildCreateRequestDto;
import ru.taskhero.userservice.dto.ChildDetailDto;
import ru.taskhero.userservice.dto.ChildResponseDto;
import ru.taskhero.userservice.dto.UpdateChildRequest;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.Parent;
import ru.taskhero.userservice.entity.AuditAction;
import ru.taskhero.userservice.mapper.ChildMapper;
import ru.taskhero.userservice.mapper.ParentMapper;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.ParentRepository;
import ru.taskhero.userservice.service.AuditService;
import ru.taskhero.userservice.service.ChildService;
import ru.taskhero.userservice.service.LevelRewardService;
import ru.taskhero.userservice.util.TokenGenerator;

import java.util.List;
import java.util.UUID;

/**
 * Имплементация ChildService для работы с сущностью Ребёнка
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChildServiceImpl implements ChildService {

    private final ChildRepository childRepository;
    private final ParentRepository parentRepository;
    private final ChildMapper childMapper;
    private final ParentMapper parentMapper;
    private final LevelRewardService levelRewardService;
    private final AuditService auditService;

    /**
     * Добавить нового ребёнка к родителю.
     *
     * @param parentId ID родителя
     * @param request  данные для создания ребёнка
     * @return DTO ребёнка
     * @throws ResourceNotFoundException если родитель не найден
     */
    @Override
    @Transactional
    @LogMethod("child-create")
    public ChildResponseDto createChild(UUID parentId, ChildCreateRequestDto request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> {
                    log.error("Родитель с ID: {} не найден при создании ребёнка.", parentId);
                    return new ResourceNotFoundException("Родитель с ID " + parentId + " не найден");
                });

        // Генерация уникального loginToken
        String loginToken = generateUniqueLoginToken();

        // Определяем траекторию: если не указана, NORMAL по умолчанию
        DifficultyTrajectory trajectory = request.difficultyTrajectory() != null
                ? request.difficultyTrajectory() : DifficultyTrajectory.NORMAL;

        Child child = Child.builder()
                .parent(parent)
                .firstName(request.firstName())
                .surname(request.surname())
                .exp(0)
                .coins(0)
                .level(1)
                .loginToken(loginToken)
                .difficultyTrajectory(trajectory)
                .build();

        child = childRepository.save(child);
        log.info("Создан ребёнок: {} {} с loginToken: {}", child.getFirstName(), child.getSurname(), loginToken);

        // Аудит
        auditService.log(parent.getUser().getId(), null, AuditAction.CHILD_CREATED,
                "CHILD", child.getId(), "Создан ребёнок " + child.getFirstName() + " " + child.getSurname());

        return toChildResponseDto(child);
    }

    /**
     * Получить всех детей родителя.
     *
     * @param parentId ID родителя
     * @return список DTO детей
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("child-get-by-parent")
    public List<ChildResponseDto> getChildrenByParent(UUID parentId) {
        List<Child> children = childRepository.findAllByParentId(parentId);
        log.debug("Найдено {} детей для родителя с ID: {}", children.size(), parentId);

        return children.stream()
                .map(this::toChildResponseDto)
                .toList();
    }

    /**
     * Найти ребёнка по loginToken.
     *
     * @param token loginToken ребёнка
     * @return DTO ребёнка
     * @throws ResourceNotFoundException если ребёнок не найден
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("child-get-by-token")
    public ChildResponseDto getByLoginToken(String token) {
        Child child = childRepository.findByLoginToken(token)
                .orElseThrow(() -> {
                    log.error("Ребёнок с loginToken: {} не найден.", token);
                    return new ResourceNotFoundException("Ребёнок с таким токеном не найден");
                });

        log.debug("Найден ребёнок: {} {} по токену", child.getFirstName(), child.getSurname());
        return toChildResponseDto(child);
    }

    /**
     * Получить всех детей в системе с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница детей
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("child-get-all")
    public Page<ChildResponseDto> getAllChildren(Pageable pageable) {
        log.info("Получение списка всех детей, page={}", pageable.getPageNumber());

        return childRepository.findAllWithParent(pageable)
                .map(this::toChildResponseDto);
    }

    /**
     * Получить детальную информацию о ребенке.
     *
     * @param childId ID ребенка
     * @return детальное DTO ребенка
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("child-get-detail")
    public ChildDetailDto getDetailById(UUID childId) {
        log.info("Получение детальной информации о ребенке: {}", childId);

        Child child = childRepository.findByIdWithParent(childId)
                .orElseThrow(() -> {
                    log.error("Ребенок с ID: {} не найден.", childId);
                    return new ResourceNotFoundException("Ребенок с ID " + childId + " не найден");
                });

        // Маппим parent вручную, чтобы избежать циклической зависимости
        return new ChildDetailDto(
                child.getId(),
                child.getFirstName(),
                child.getLoginToken(),
                child.getExp(),
                child.getCoins(),
                child.getLevel(),
                parentMapper.toParentResponseDto(child.getParent()),
                child.getCreatedAt(),
                child.getUpdatedAt()
        );
    }

    /**
     * Получить ребенка по ID.
     *
     * @param childId ID ребенка
     * @return DTO ребенка
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("child-get-by-id")
    public ChildResponseDto getById(UUID childId) {
        log.info("Получение ребенка по ID: {}", childId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребенок с ID " + childId + " не найден"));

        return toChildResponseDto(child);
    }

    /**
     * Обновить данные ребенка.
     *
     * @param childId ID ребенка
     * @param request данные для обновления
     * @return обновленное DTO ребенка
     */
    @Override
    @Transactional
    @LogMethod("child-update")
    public ChildResponseDto updateChild(UUID childId, UpdateChildRequest request) {
        log.info("Обновление данных ребенка: {}", childId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребенок с ID " + childId + " не найден"));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            child.setFirstName(request.firstName());
        }
        if (request.exp() != null) {
            child.setExp(request.exp());
        }
        if (request.coins() != null) {
            child.setCoins(request.coins());
        }
        if (request.level() != null) {
            child.setLevel(request.level());
        }

        child = childRepository.save(child);
        log.info("Данные ребенка {} обновлены", childId);

        return toChildResponseDto(child);
    }

    /**
     * Удалить ребенка.
     *
     * @param childId ID ребенка
     */
    @Override
    @Transactional
    @LogMethod("child-delete")
    public void deleteChild(UUID childId) {
        log.info("Удаление ребенка: {}", childId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребенок с ID " + childId + " не найден"));

        childRepository.delete(child);
        log.info("Ребенок {} удален", childId);
    }

    /**
     * Проверка, принадлежит ли ребенок родителю.
     *
     * @param childId  ID ребенка
     * @param parentId ID родителя
     * @return true если ребенок принадлежит родителю
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("child-check-ownership")
    public boolean isChildBelongsToParent(UUID childId, UUID parentId) {
        if (!childRepository.existsById(childId)) {
            throw new ResourceNotFoundException("Ребенок с ID " + childId + " не найден");
        }
        return childRepository.existsByIdAndParentId(childId, parentId);
    }

    /**
     * Начислить награду ребёнку (EXP и коины).
     *
     * @param childId ID ребёнка
     * @param exp     количество EXP для начисления
     * @param coins   количество коинов для начисления
     * @return обновленные данные ребёнка
     */
    @Override
    @Transactional
    @LogMethod("child-add-reward")
    public ChildResponseDto addReward(UUID childId, int exp, int coins) {
        return addReward(childId, exp, coins, false);
    }

    @Override
    @Transactional
    @LogMethod("child-add-reward")
    public ChildResponseDto addReward(UUID childId, int exp, int coins, boolean capExp) {
        log.info("Начисление награды ребёнку {}: {} EXP, {} коинов, capExp={}", childId, exp, coins, capExp);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> {
                    log.error("Ребёнок с ID {} не найден при начислении награды", childId);
                    return new ResourceNotFoundException("Ребёнок с ID " + childId + " не найден");
                });

        // Применяем множитель траектории к EXP
        DifficultyTrajectory trajectory = child.getDifficultyTrajectory() != null
                ? child.getDifficultyTrajectory() : DifficultyTrajectory.NORMAL;
        int adjustedExp = (int) Math.round(exp * trajectory.getExpMultiplier());

        // Начисляем награды
        int newExp = child.getExp() + adjustedExp;

        // Если есть незавершённые важные задания — ограничиваем EXP максимумом текущего уровня
        if (capExp) {
            int nextLevelThreshold = getExpForLevel(child.getLevel() + 1);
            if (newExp >= nextLevelThreshold) {
                newExp = nextLevelThreshold - 1;
                log.info("EXP ограничен до {} из-за незавершённых важных заданий", newExp);
            }
        }

        child.setExp(newExp);
        child.setCoins(child.getCoins() + coins);

        // Пересчитываем уровень
        int newLevel = calculateLevel(child.getExp());
        if (newLevel > child.getLevel()) {
            log.info("Ребёнок {} повысил уровень: {} -> {}", childId, child.getLevel(), newLevel);
            child.setLevel(newLevel);

            // Проверяем и начисляем награды за достигнутые уровни
            levelRewardService.checkAndClaimRewards(childId, newLevel);
        }

        child = childRepository.save(child);
        log.info("Награда начислена ребёнку {}. Новый EXP: {}, коины: {}, уровень: {}",
                childId, child.getExp(), child.getCoins(), child.getLevel());

        return toChildResponseDto(child);
    }

    /**
     * Выбрать персонажа для ребёнка (при первом входе).
     *
     * @param childId       ID ребёнка
     * @param characterType тип персонажа
     * @return обновленные данные ребёнка
     */
    @Override
    @Transactional
    @LogMethod("child-select-character")
    public ChildResponseDto selectCharacter(UUID childId, CharacterType characterType) {
        log.info("Выбор персонажа {} для ребёнка {}", characterType, childId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребёнок с ID " + childId + " не найден"));

        if (child.isCharacterSelected()) {
            throw new ValidationException("Персонаж уже выбран");
        }

        child.setCharacterType(characterType);
        child.setCharacterSelected(true);
        child = childRepository.save(child);

        log.info("Ребёнок {} выбрал персонажа {}", childId, characterType);
        return toChildResponseDto(child);
    }

    /**
     * Рассчитать уровень по количеству EXP.
     * Формула: каждый уровень требует progressively больше EXP.
     * Траектория теперь влияет на множитель получаемого EXP, а не на пороги.
     *
     * @param exp количество EXP
     * @return уровень
     */
    private int calculateLevel(int exp) {
        if (exp < 50) {
            return 1;
        }

        int level = 1;
        int expRequired = 0;

        while (expRequired <= exp) {
            level++;
            expRequired += 40 + 5 * level * (level - 1);
        }

        return level - 1;
    }

    /**
     * Рассчитать общий EXP для достижения уровня.
     *
     * @param level целевой уровень
     * @return необходимый EXP
     */
    private int getExpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }

        int totalExp = 0;
        for (int i = 2; i <= level; i++) {
            totalExp += 40 + 5 * i * (i - 1);
        }

        return totalExp;
    }

    /**
     * Конвертировать Child в ChildResponseDto с расчётом EXP-прогресса.
     * currentLevelExp — прогресс EXP внутри текущего уровня (от 0 до nextLevelExp).
     * nextLevelExp — общее количество EXP, необходимое для перехода с текущего на следующий уровень.
     *
     * @param child сущность ребёнка
     * @return DTO ребёнка
     */
    private ChildResponseDto toChildResponseDto(Child child) {
        int currentLevelThreshold = getExpForLevel(child.getLevel());
        int nextLevelThreshold = getExpForLevel(child.getLevel() + 1);
        int levelRange = nextLevelThreshold - currentLevelThreshold;
        int progressInLevel = Math.max(0, child.getExp() - currentLevelThreshold);
        int expToNextLevel = Math.max(0, nextLevelThreshold - child.getExp());

        var parent = child.getParent() != null ? parentMapper.toParentResponseDto(child.getParent()) : null;
        return childMapper.toDto(child, expToNextLevel, progressInLevel, levelRange, parent);
    }

    /**
     * Генерация уникального loginToken с проверкой на коллизии.
     *
     * @return уникальный токен
     */
    private String generateUniqueLoginToken() {
        String token;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            token = TokenGenerator.generateLoginToken();
            attempts++;

            if (attempts >= maxAttempts) {
                log.error("Не удалось сгенерировать уникальный loginToken за {} попыток", maxAttempts);
                throw new RuntimeException("Не удалось сгенерировать уникальный токен");
            }
        } while (childRepository.findByLoginToken(token).isPresent());

        log.debug("Сгенерирован уникальный loginToken за {} попыток", attempts);
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChildResponseDto> searchChildren(String query, Pageable pageable) {
        log.info("Поиск детей по запросу: {}", query);
        return childRepository.searchByName(query, pageable)
                .map(this::toChildResponseDto);
    }
}
