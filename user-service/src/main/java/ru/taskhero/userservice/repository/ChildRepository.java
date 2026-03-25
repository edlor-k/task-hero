package ru.taskhero.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Получить ребёнка по ID с загруженным родителем.
     */
    @Query("SELECT c FROM Child c LEFT JOIN FETCH c.parent WHERE c.id = :id")
    Optional<Child> findByIdWithParent(@Param("id") UUID id);

    /**
     * Проверить, принадлежит ли ребёнок родителю (без загрузки сущностей).
     */
    @Query("SELECT COUNT(c) > 0 FROM Child c WHERE c.id = :childId AND c.parent.id = :parentId")
    boolean existsByIdAndParentId(@Param("childId") UUID childId, @Param("parentId") UUID parentId);

    /**
     * Поиск детей по имени или фамилии (для админки).
     */
    @Query("SELECT c FROM Child c LEFT JOIN FETCH c.parent "
            + "WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(c.surname) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Child> searchByName(@Param("q") String query, Pageable pageable);
}
