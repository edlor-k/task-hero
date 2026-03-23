package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.aop.LogMethod;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.ParentResponseDto;
import ru.taskhero.userservice.dto.RegisterResponseDto;
import ru.taskhero.userservice.dto.UserRegisterRequest;
import ru.taskhero.userservice.dto.UserResponseDto;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.mapper.UserMapper;
import ru.taskhero.userservice.repository.UserRepository;
import ru.taskhero.userservice.service.ParentService;
import ru.taskhero.userservice.service.UserService;

import java.util.UUID;

/**
 * Имплементация UserService для управления регистрацией пользователей.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ParentService parentService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Регистрация нового пользователя (родителя).
     * В одной транзакции создаются User и связанный Parent профиль.
     *
     * @param request данные для регистрации
     * @return DTO с данными пользователя и профиля родителя
     * @throws ValidationException если email уже существует
     */
    @Override
    @Transactional
    @LogMethod("user-create")
    public RegisterResponseDto register(UserRegisterRequest request) {
        log.info("Начало регистрации пользователя с email: {}", request.email());

        // Проверка на существование email
        if (userRepository.existsByEmail(request.email())) {
            log.error("Пользователь с email: {} уже существует.", request.email());
            throw new ValidationException("Пользователь с таким email уже существует");
        }

        // Создание User
        User user = userMapper.toEntity(request);
        user.setEmail(request.email()); // явно устанавливаем email
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.PARENT);

        log.debug("User перед сохранением: email={}, role={}", user.getEmail(), user.getRole());

        user = userRepository.save(user);
        log.info("Создан новый пользователь: {}", user.getEmail());

        // Создание Parent профиля
        ParentResponseDto parentDto = parentService.createForUser(user.getId(), request.firstName(), request.surname());
        log.info("Создан профиль родителя для пользователя: {}", user.getId());

        // Формируем комбинированный ответ
        return new RegisterResponseDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                parentDto.id(),
                parentDto.firstName(),
                parentDto.surname(),
                user.isActive()
        );
    }

    /**
     * Поиск пользователя по ID.
     *
     * @param id идентификатор пользователя
     * @return DTO пользователя
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("user-get-by-id")
    public UserResponseDto getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID: {} не найден.", id);
                    return new ResourceNotFoundException("Пользователь с ID " + id + " не найден");
                });

        log.debug("Найден пользователь: {}", user.getEmail());
        return userMapper.toDto(user);
    }

    /**
     * Проверка существования пользователя по email.
     *
     * @param email email пользователя
     * @return true если пользователь существует
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("user-exists")
    public boolean existsByEmail(String email) {
        boolean exists = userRepository.existsByEmail(email);
        log.debug("Проверка существования email: {} = {}", email, exists);
        return exists;
    }

    /**
     * Получить всех пользователей с пагинацией и фильтрами.
     *
     * @param pageable параметры пагинации
     * @param role     фильтр по роли (опционально)
     * @param active   фильтр по активности (опционально)
     * @return страница пользователей
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("user-get-all")
    public org.springframework.data.domain.Page<UserResponseDto> getAllUsers(
            org.springframework.data.domain.Pageable pageable, Role role, Boolean active) {
        log.info("Получение списка пользователей: role={}, active={}, page={}", role, active, pageable.getPageNumber());

        org.springframework.data.domain.Page<User> users;

        if (role != null && active != null) {
            users = userRepository.findByRoleAndActive(role, active, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(role, pageable);
        } else if (active != null) {
            users = userRepository.findByActive(active, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(userMapper::toDto);
    }

    /**
     * Переключить активность пользователя.
     *
     * @param userId ID пользователя
     * @return обновленный пользователь
     */
    @Override
    @Transactional
    @LogMethod("user-toggle-active")
    public UserResponseDto toggleActive(UUID userId) {
        log.info("Переключение активности пользователя: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с ID " + userId + " не найден"));

        boolean newActiveState = !user.isActive();
        user.setActive(newActiveState);
        user = userRepository.save(user);

        log.info("Пользователь {} теперь {}", userId, newActiveState ? "активен" : "заблокирован");
        return userMapper.toDto(user);
    }

    /**
     * Изменить роль пользователя.
     *
     * @param userId  ID пользователя
     * @param newRole новая роль
     * @return обновленный пользователь
     */
    @Override
    @Transactional
    @LogMethod("user-update-role")
    public UserResponseDto updateRole(UUID userId, Role newRole) {
        log.info("Изменение роли пользователя {}: новая роль={}", userId, newRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с ID " + userId + " не найден"));

        if (user.getRole() == newRole) {
            log.warn("Пользователь {} уже имеет роль {}", userId, newRole);
            throw new ValidationException("Пользователь уже имеет роль " + newRole);
        }

        user.setRole(newRole);
        user = userRepository.save(user);

        log.info("Роль пользователя {} изменена на {}", userId, newRole);
        return userMapper.toDto(user);
    }

    /**
     * Удалить пользователя (soft delete).
     *
     * @param userId ID пользователя
     */
    @Override
    @Transactional
    @LogMethod("user-delete")
    public void deleteUser(UUID userId) {
        log.info("Удаление пользователя: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с ID " + userId + " не найден"));

        if (!user.isActive()) {
            log.warn("Пользователь {} уже неактивен", userId);
            throw new ValidationException("Пользователь уже неактивен");
        }

        user.setActive(false);
        userRepository.save(user);

        log.info("Пользователь {} помечен как удаленный", userId);
    }

    /**
     * Поиск пользователей по email.
     *
     * @param search   строка поиска
     * @param pageable параметры пагинации
     * @return страница пользователей
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("user-search")
    public Page<UserResponseDto> searchByEmail(String search, Pageable pageable) {
        log.info("Поиск пользователей по email: search={}", search);

        return userRepository.searchByEmail(search, pageable)
                .map(userMapper::toDto);
    }

    @Override
    @Transactional
    @LogMethod("user-reset-password")
    public void resetPassword(UUID userId, String newPassword) {
        log.info("Сброс пароля пользователя: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с ID " + userId + " не найден"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Пароль пользователя {} успешно сброшен", userId);
    }
}
