package ru.taskhero.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.taskhero.userservice.entity.AvatarOption;

import java.util.List;
import java.util.UUID;

/**
 * Jpa репозиторий для галереи аватаров.
 */
public interface AvatarOptionRepository extends JpaRepository<AvatarOption, UUID> {

    /**
     * Активные опции аватара, отсортированные для пикера ребёнка.
     */
    @Query("SELECT a FROM AvatarOption a WHERE a.active = true ORDER BY a.sortOrder ASC, a.createdAt ASC")
    List<AvatarOption> findAllActiveOrdered();

    /**
     * Все опции (вкл. неактивные) для админки.
     */
    @Query("SELECT a FROM AvatarOption a ORDER BY a.sortOrder ASC, a.createdAt ASC")
    List<AvatarOption> findAllOrdered();
}
