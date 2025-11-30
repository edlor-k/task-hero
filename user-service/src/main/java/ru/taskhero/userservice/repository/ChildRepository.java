package ru.taskhero.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.taskhero.userservice.entity.Child;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Jpa репозиторий для работы с сущностью ребенка.
 */
public interface ChildRepository extends JpaRepository<Child, UUID> {
    /**
     * Список детей, принадлежащих родителю.
     */
    List<Child> findAllByParentId(UUID parentId);

    /**
     * Поиск ребенка по loginToken.
     */
    Optional<Child> findByLoginToken(String loginToken);

    /**
     * Получить всех детей с пагинацией.
     */
    Page<Child> findAll(Pageable pageable);

    /**
     * Получить всех детей с информацией о родителях (join fetch для оптимизации).
     */
    @Query("SELECT c FROM Child c LEFT JOIN FETCH c.parent")
    Page<Child> findAllWithParent(Pageable pageable);
}
