package ru.taskhero.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.taskhero.userservice.entity.LevelReward;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LevelRewardRepository extends JpaRepository<LevelReward, UUID> {

    List<LevelReward> findAllByChildIdOrderByLevel(UUID childId);

    List<LevelReward> findAllByChildIdAndClaimedFalseOrderByLevel(UUID childId);

    List<LevelReward> findAllByChildIdAndClaimedTrueOrderByLevel(UUID childId);

    Optional<LevelReward> findByChildIdAndLevel(UUID childId, int level);

    int countByChildIdAndClaimedFalse(UUID childId);

    boolean existsByChildIdAndLevel(UUID childId, int level);

    @Query("SELECT COALESCE(MAX(lr.level), 0) FROM LevelReward lr WHERE lr.child.id = :childId")
    int findMaxLevelByChildId(@Param("childId") UUID childId);

    List<LevelReward> findAllByChildIdAndClaimedTrueAndSeenFalse(UUID childId);

    /**
     * Найти награду по ID с загруженным ребёнком.
     */
    @Query("SELECT lr FROM LevelReward lr JOIN FETCH lr.child WHERE lr.id = :id")
    Optional<LevelReward> findByIdWithChild(@Param("id") UUID id);

    /**
     * Найти награду по ID с загруженным ребёнком и его родителем.
     */
    @Query("SELECT lr FROM LevelReward lr JOIN FETCH lr.child c LEFT JOIN FETCH c.parent WHERE lr.id = :id")
    Optional<LevelReward> findByIdWithChildAndParent(@Param("id") UUID id);
}
