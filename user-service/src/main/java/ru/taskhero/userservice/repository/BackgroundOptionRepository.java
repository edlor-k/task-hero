package ru.taskhero.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.taskhero.userservice.entity.BackgroundOption;

import java.util.List;
import java.util.UUID;

public interface BackgroundOptionRepository extends JpaRepository<BackgroundOption, UUID> {

    @Query("SELECT b FROM BackgroundOption b WHERE b.active = true ORDER BY b.sortOrder ASC, b.createdAt ASC")
    List<BackgroundOption> findAllActiveOrdered();

    @Query("SELECT b FROM BackgroundOption b ORDER BY b.sortOrder ASC, b.createdAt ASC")
    List<BackgroundOption> findAllOrdered();
}
