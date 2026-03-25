package ru.taskhero.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.taskhero.userservice.entity.Parent;

import java.util.Optional;
import java.util.UUID;

/**
 * Jpa репозиторий для работы с сущностью родителя.
 */
public interface ParentRepository extends JpaRepository<Parent, UUID> {
    /**
     * Поиск профиля родителя по ID пользователя.
     */
    Optional<Parent> findByUserId(UUID userId);

    /**
     * Получить всех родителей с пагинацией.
     */
    Page<Parent> findAll(Pageable pageable);

    /**
     * Получить всех родителей с их детьми (с join fetch для оптимизации).
     */
    @Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children LEFT JOIN FETCH p.user")
    Page<Parent> findAllWithChildren(Pageable pageable);

    /**
     * Получить родителя по ID с загруженными детьми и пользователем.
     */
    @Query("SELECT p FROM Parent p LEFT JOIN FETCH p.children LEFT JOIN FETCH p.user WHERE p.id = :id")
    Optional<Parent> findByIdWithChildrenAndUser(@Param("id") UUID id);

    /**
     * Поиск родителей по имени, фамилии или email (для админки).
     */
    @Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children LEFT JOIN FETCH p.user "
            + "WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(p.surname) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(p.user.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Parent> searchByNameOrEmail(@Param("q") String query, Pageable pageable);
}
