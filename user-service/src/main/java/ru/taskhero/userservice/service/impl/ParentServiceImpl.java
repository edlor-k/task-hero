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
import ru.taskhero.userservice.dto.ParentDetailDto;
import ru.taskhero.userservice.dto.ParentResponseDto;
import ru.taskhero.userservice.dto.ParentWithChildrenDto;
import ru.taskhero.userservice.dto.UpdateParentRequest;
import ru.taskhero.userservice.entity.Parent;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.mapper.ChildMapper;
import ru.taskhero.userservice.mapper.ParentMapper;
import ru.taskhero.userservice.mapper.UserMapper;
import ru.taskhero.userservice.repository.ParentRepository;
import ru.taskhero.userservice.repository.UserRepository;
import ru.taskhero.userservice.service.ParentService;

import java.util.UUID;

/**
 * Имплементация ParentService для работы с сущностью Родителя
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParentServiceImpl implements ParentService {

    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final ParentMapper parentMapper;
    private final ChildMapper childMapper;
    private final UserMapper userMapper;

    /**
     * Создание профиля родителя на основе User ID.
     *
     * @param userId    ID пользователя
     * @param firstName имя родителя
     * @param surname   фамилия родителя
     * @return DTO родителя
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Override
    @Transactional
    @LogMethod("parent-create")
    public ParentResponseDto createForUser(UUID userId, String firstName, String surname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID: {} не найден при создании профиля родителя.", userId);
                    return new ResourceNotFoundException("Пользователь с ID " + userId + " не найден");
                });

        Parent parent = Parent.builder()
                .user(user)
                .firstName(firstName)
                .surname(surname)
                .build();

        parent = parentRepository.save(parent);
        log.info("Создан профиль родителя для пользователя: {}", userId);

        return parentMapper.toParentResponseDto(parent);
    }

    /**
     * Получение информации о родителе по User ID.
     *
     * @param userId ID пользователя
     * @return DTO родителя
     * @throws ResourceNotFoundException если родитель не найден
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("parent-get-by-user-id")
    public ParentResponseDto getByUserId(UUID userId) {
        Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Профиль родителя для пользователя с ID: {} не найден.", userId);
                    return new ResourceNotFoundException("Профиль родителя не найден для пользователя " + userId);
                });

        log.debug("Найден профиль родителя для пользователя: {}", userId);
        return parentMapper.toParentResponseDto(parent);
    }

    /**
     * Получить всех родителей с информацией о детях.
     *
     * @param pageable параметры пагинации
     * @return страница родителей с детьми
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("parent-get-all")
    public Page<ParentWithChildrenDto> getAllParents(Pageable pageable) {
        log.info("Получение списка всех родителей, page={}", pageable.getPageNumber());

        return parentRepository.findAll(pageable)
                .map(parent -> new ParentWithChildrenDto(
                        parent.getId(),
                        parent.getFirstName(),
                        parent.getSurname(),
                        userMapper.toDto(parent.getUser()),
                        parent.getChildren().stream()
                                .map(childMapper::toDto)
                                .toList(),
                        parent.getChildren() != null ? parent.getChildren().size() : 0
                ));
    }

    /**
     * Получить детальную информацию о родителе.
     *
     * @param parentId ID родителя
     * @return детальное DTO родителя
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("parent-get-detail")
    public ParentDetailDto getDetailById(UUID parentId) {
        log.info("Получение детальной информации о родителе: {}", parentId);

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> {
                    log.error("Родитель с ID: {} не найден.", parentId);
                    return new ResourceNotFoundException("Родитель с ID " + parentId + " не найден");
                });

        return new ParentDetailDto(
                parent.getId(),
                parent.getFirstName(),
                parent.getSurname(),
                userMapper.toDto(parent.getUser()),
                parent.getChildren().stream()
                        .map(childMapper::toDto)
                        .toList(),
                parent.getCreatedAt(),
                parent.getUpdatedAt()
        );
    }

    /**
     * Обновить профиль родителя (для администраторов).
     *
     * @param parentId ID родителя
     * @param request  данные для обновления
     * @return обновленный профиль
     */
    @Override
    @Transactional
    @LogMethod("parent-update")
    public ParentResponseDto updateParent(UUID parentId, UpdateParentRequest request) {
        log.info("Обновление профиля родителя: {}", parentId);

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Родитель с ID " + parentId + " не найден"));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            parent.setFirstName(request.firstName());
        }
        if (request.surname() != null && !request.surname().isBlank()) {
            parent.setSurname(request.surname());
        }

        parent = parentRepository.save(parent);
        log.info("Профиль родителя {} обновлен", parentId);

        return parentMapper.toParentResponseDto(parent);
    }

    /**
     * Обновить профиль текущего родителя.
     *
     * @param userId  ID пользователя
     * @param request данные для обновления
     * @return обновленный профиль
     */
    @Override
    @Transactional
    @LogMethod("parent-update-current")
    public ParentResponseDto updateCurrentParent(UUID userId, UpdateParentRequest request) {
        log.info("Обновление профиля текущего родителя, userId: {}", userId);

        Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль родителя не найден для пользователя " + userId));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            parent.setFirstName(request.firstName());
        }
        if (request.surname() != null && !request.surname().isBlank()) {
            parent.setSurname(request.surname());
        }

        parent = parentRepository.save(parent);
        log.info("Профиль родителя обновлен для пользователя {}", userId);

        return parentMapper.toParentResponseDto(parent);
    }
}
