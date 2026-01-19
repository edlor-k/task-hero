package ru.taskhero.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
