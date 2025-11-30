package ru.taskhero.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Jpa репозиторий для работы с сущностью обобщенного пользователя.
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Поиск пользователя по email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверка существования пользователя по email.
     */
    boolean existsByEmail(String email);

    /**
     * Получить всех пользователей с пагинацией.
     */
    Page<User> findAll(Pageable pageable);

    /**
     * Получить пользователей по роли.
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Получить пользователей по активности.
     */
    Page<User> findByActive(boolean active, Pageable pageable);

    /**
     * Получить пользователей по роли и активности.
     */
    Page<User> findByRoleAndActive(Role role, boolean active, Pageable pageable);

    /**
     * Поиск пользователей по email (частичное совпадение).
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchByEmail(@Param("search") String search, Pageable pageable);

    /**
     * Подсчет активных пользователей.
     */
    long countByActive(boolean active);

    /**
     * Подсчет пользователей по роли.
     */
    long countByRole(Role role);
}
