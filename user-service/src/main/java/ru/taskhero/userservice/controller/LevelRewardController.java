package ru.taskhero.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.taskhero.userservice.dto.LevelInfoDto;
import ru.taskhero.userservice.dto.LevelRewardBulkCreateRequest;
import ru.taskhero.userservice.dto.LevelRewardResponseDto;
import ru.taskhero.userservice.service.LevelRewardService;
import ru.taskhero.userservice.service.ParentService;
import ru.taskhero.userservice.util.SecurityUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер для наград за достижение уровней.
 */
@Slf4j
@RestController
@RequestMapping("/level-rewards")
@RequiredArgsConstructor
@Tag(name = "Level Rewards", description = "Награды за достижение уровней")
@SecurityRequirement(name = "Bearer Authentication")
public class LevelRewardController {

    private final LevelRewardService levelRewardService;
    private final ParentService parentService;

    /**
     * Создать награды за уровни (bulk).
     */
    @PostMapping("/children/{childId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Создать награды за уровни", description = "Родитель создаёт награды для ребёнка за достижение уровней")
    public ResponseEntity<List<LevelRewardResponseDto>> createRewards(
            @PathVariable UUID childId,
            @Valid @RequestBody LevelRewardBulkCreateRequest request
    ) {
        UUID parentId = getParentId();
        List<LevelRewardResponseDto> response = levelRewardService.createRewards(childId, parentId, request.rewards());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получить все награды ребёнка.
     */
    @GetMapping("/children/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'CHILD')")
    @Operation(summary = "Награды ребёнка", description = "Получить все награды за уровни для ребёнка")
    public ResponseEntity<List<LevelRewardResponseDto>> getRewardsByChild(@PathVariable UUID childId) {
        return ResponseEntity.ok(levelRewardService.getRewardsByChild(childId));
    }

    /**
     * Получить историю полученных наград.
     */
    @GetMapping("/children/{childId}/history")
    @PreAuthorize("hasAnyRole('PARENT', 'CHILD')")
    @Operation(summary = "История наград", description = "Получить полученные награды за уровни")
    public ResponseEntity<List<LevelRewardResponseDto>> getRewardHistory(@PathVariable UUID childId) {
        return ResponseEntity.ok(levelRewardService.getRewardHistory(childId));
    }

    /**
     * Получить непросмотренные награды ребёнка.
     */
    @GetMapping("/children/{childId}/unseen")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Непросмотренные награды", description = "Получить награды, которые ребёнок ещё не видел")
    public ResponseEntity<List<LevelRewardResponseDto>> getUnseenRewards(@PathVariable UUID childId) {
        return ResponseEntity.ok(levelRewardService.getUnseenRewards(childId));
    }

    /**
     * Пометить награду как просмотренную.
     */
    @PostMapping("/{id}/seen")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Пометить как просмотренное")
    public ResponseEntity<LevelRewardResponseDto> markSeen(@PathVariable UUID id) {
        UUID childId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(levelRewardService.markRewardSeen(id, childId));
    }

    /**
     * Отметить награду как выданную родителем.
     */
    @PostMapping("/{id}/issued")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Отметить выдачу", description = "Родитель подтверждает, что награда выдана ребёнку")
    public ResponseEntity<LevelRewardResponseDto> markIssued(@PathVariable UUID id) {
        UUID parentId = getParentId();
        return ResponseEntity.ok(levelRewardService.markRewardIssued(id, parentId));
    }

    /**
     * Удалить награду (только незаклейменную).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Удалить награду", description = "Удалить награду за уровень (только если ещё не получена)")
    public ResponseEntity<Void> deleteReward(@PathVariable UUID id) {
        UUID parentId = getParentId();
        levelRewardService.deleteReward(id, parentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Информация об уровнях (XP, примерное количество заданий).
     */
    @GetMapping("/children/{childId}/level-info")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Информация об уровнях", description = "XP и примерное количество заданий для каждого уровня")
    public ResponseEntity<List<LevelInfoDto>> getLevelInfo(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "2") int from,
            @RequestParam(defaultValue = "11") int to
    ) {
        return ResponseEntity.ok(levelRewardService.getLevelInfo(childId, from, to));
    }

    /**
     * Количество уровней без наград впереди ребёнка.
     */
    @GetMapping("/children/{childId}/unfilled-count")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Незаполненные уровни", description = "Сколько уровней впереди ребёнка без награды")
    public ResponseEntity<Map<String, Integer>> getUnfilledCount(@PathVariable UUID childId) {
        return ResponseEntity.ok(levelRewardService.getUnfilledLevelCount(childId));
    }

    private UUID getParentId() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return parentService.getByUserId(userId).id();
    }
}
