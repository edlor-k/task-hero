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
import ru.taskhero.userservice.dto.ChildCreateRequestDto;
import ru.taskhero.userservice.dto.ChildDetailDto;
import ru.taskhero.userservice.dto.ChildResponseDto;
import ru.taskhero.userservice.dto.UpdateChildRequest;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.Parent;
import ru.taskhero.userservice.mapper.ChildMapper;
import ru.taskhero.userservice.mapper.ParentMapper;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.ParentRepository;
import ru.taskhero.userservice.service.ChildService;
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

        Child child = Child.builder()
                .parent(parent)
                .firstName(request.firstName())
                .surname(request.surname())
                .exp(0)
                .coins(0)
                .level(1)
                .loginToken(loginToken)
                .build();

        child = childRepository.save(child);
        log.info("Создан ребёнок: {} {} с loginToken: {}", child.getFirstName(), child.getSurname(), loginToken);

        return childMapper.toDto(child);
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
                .map(childMapper::toDto)
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
        return childMapper.toDto(child);
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

        return childRepository.findAll(pageable)
                .map(childMapper::toDto);
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

        Child child = childRepository.findById(childId)
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

        return childMapper.toDto(child);
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

        return childMapper.toDto(child);
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
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребенок с ID " + childId + " не найден"));

        return child.getParent() != null && child.getParent().getId().equals(parentId);
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
}
