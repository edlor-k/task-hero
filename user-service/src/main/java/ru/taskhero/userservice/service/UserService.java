package ru.taskhero.userservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.RegisterResponseDto;
import ru.taskhero.userservice.dto.UserRegisterRequest;
import ru.taskhero.userservice.dto.UserResponseDto;

import java.util.UUID;

public interface UserService {

    /**
     * Регистрация нового пользователя (родителя).
     * Создает User и связанный Parent профиль.
     */
    RegisterResponseDto register(UserRegisterRequest request);

    /**
     * Поиск пользователя по ID.
     */
    UserResponseDto getById(UUID id);

    /**
     * Проверка существования пользователя по email.
     */
    boolean existsByEmail(String email);

    /**
     * Получить всех пользователей с пагинацией и фильтрами.
     */
    Page<UserResponseDto> getAllUsers(Pageable pageable, Role role, Boolean active);

    /**
     * Переключить активность пользователя (блокировка/разблокировка).
     */
    UserResponseDto toggleActive(UUID userId);

    /**
     * Изменить роль пользователя.
     */
    UserResponseDto updateRole(UUID userId, Role newRole);

    /**
     * Удалить пользователя (soft delete - устанавливает is_active=false).
     */
    void deleteUser(UUID userId);

    /**
     * Поиск пользователей по email.
     */
    Page<UserResponseDto> searchByEmail(String search, Pageable pageable);

    /**
     * Сброс пароля пользователя администратором.
     */
    void resetPassword(UUID userId, String newPassword);
}
