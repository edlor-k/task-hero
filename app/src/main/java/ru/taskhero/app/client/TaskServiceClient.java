package ru.taskhero.app.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.taskhero.app.dto.TaskAssignmentDto;
import ru.taskhero.app.dto.TaskTemplateDto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Feign клиент для Task Service.
 */
@FeignClient(
        name = "task-service",
        url = "${services.task-service.url}"
)
public interface TaskServiceClient {

    // ==================== Templates ====================

    /**
     * Получить список шаблонов родителя.
     */
    @GetMapping("/templates")
    Map<String, Object> getTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category
    );

    /**
     * Получить активные шаблоны.
     */
    @GetMapping("/templates/active")
    List<TaskTemplateDto> getActiveTemplates();

    /**
     * Получить шаблон по ID.
     */
    @GetMapping("/templates/{id}")
    TaskTemplateDto getTemplateById(@PathVariable("id") UUID id);

    /**
     * Создать шаблон.
     */
    @PostMapping("/templates")
    TaskTemplateDto createTemplate(@RequestBody Map<String, Object> request);

    /**
     * Обновить шаблон.
     */
    @PutMapping("/templates/{id}")
    TaskTemplateDto updateTemplate(@PathVariable("id") UUID id, @RequestBody Map<String, Object> request);

    /**
     * Удалить шаблон.
     */
    @DeleteMapping("/templates/{id}")
    void deleteTemplate(@PathVariable("id") UUID id);

    /**
     * Получить шаблоны из библиотеки.
     */
    @GetMapping("/templates/library")
    List<TaskTemplateDto> getLibraryTemplates(@RequestParam(required = false) String category);

    /**
     * Скопировать шаблон из библиотеки.
     */
    @PostMapping("/templates/library/{id}/copy")
    TaskTemplateDto copyFromLibrary(@PathVariable("id") UUID id);

    /**
     * Получить ID скопированных из библиотеки шаблонов.
     */
    @GetMapping("/templates/library/copied-ids")
    Set<UUID> getCopiedLibraryTemplateIds();

    // ==================== Assignments ====================

    /**
     * Получить назначения родителя.
     */
    @GetMapping("/assignments")
    Map<String, Object> getAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    /**
     * Получить задания на проверку.
     */
    @GetMapping("/assignments/pending-review")
    List<TaskAssignmentDto> getPendingReview();

    /**
     * Получить задания ребёнка (для родителя).
     */
    @GetMapping("/assignments/child/{childId}")
    List<TaskAssignmentDto> getChildAssignments(
            @PathVariable("childId") UUID childId,
            @RequestParam(required = false) String status
    );

    /**
     * Назначить задание (по существующему шаблону).
     */
    @PostMapping("/assignments")
    TaskAssignmentDto assignTask(@RequestBody Map<String, Object> request);

    /**
     * Атомарно создать шаблон (если templateId не передан) и назначить задание —
     * одна транзакция БД на стороне task-service: либо сохраняется всё, либо ничего.
     */
    @PostMapping("/assignments/assign-with-template")
    TaskAssignmentDto assignTaskWithTemplate(@RequestBody Map<String, Object> request);

    /**
     * Одобрить задание.
     */
    @PostMapping("/assignments/{id}/approve")
    TaskAssignmentDto approveTask(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) Map<String, Object> request
    );

    /**
     * Отклонить задание.
     */
    @PostMapping("/assignments/{id}/reject")
    TaskAssignmentDto rejectTask(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) Map<String, Object> request
    );

    // ==================== Child endpoints ====================

    /**
     * Получить свои задания (для ребёнка).
     */
    @GetMapping("/assignments/my")
    List<TaskAssignmentDto> getMyAssignments(@RequestParam(required = false) String status);

    /**
     * Получить активные задания (для ребёнка).
     */
    @GetMapping("/assignments/my/active")
    List<TaskAssignmentDto> getMyActiveAssignments();

    /**
     * Сдать задание.
     */
    @PostMapping("/assignments/{id}/submit")
    TaskAssignmentDto submitTask(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) Map<String, Object> request
    );

    /**
     * Получить просроченные задания.
     */
    @GetMapping("/assignments/expired")
    List<TaskAssignmentDto> getExpiredAssignments();

    /**
     * Получить назначение по ID.
     */
    @GetMapping("/assignments/{id}")
    TaskAssignmentDto getAssignment(@PathVariable("id") UUID id);

    /**
     * Продлить дедлайн просроченного задания.
     */
    @PostMapping("/assignments/{id}/extend-deadline")
    TaskAssignmentDto extendDeadline(
            @PathVariable("id") UUID id,
            @RequestParam("newDueDate") String newDueDate
    );

    /**
     * Удалить назначение.
     */
    @DeleteMapping("/assignments/{id}")
    void deleteAssignment(@PathVariable("id") UUID id);

    // ==================== Admin Library Templates ====================

    /** Получить все шаблоны библиотеки (админ). */
    @GetMapping("/admin/templates/library")
    List<TaskTemplateDto> getAdminLibraryTemplates();

    /** Создать шаблон в библиотеке (админ). */
    @PostMapping("/admin/templates/library")
    TaskTemplateDto createLibraryTemplate(@RequestBody Map<String, Object> request);

    /** Обновить шаблон библиотеки (админ). */
    @PutMapping("/admin/templates/library/{id}")
    TaskTemplateDto updateLibraryTemplate(@PathVariable("id") UUID id, @RequestBody Map<String, Object> request);

    /** Удалить шаблон библиотеки (админ). */
    @DeleteMapping("/admin/templates/library/{id}")
    void deleteLibraryTemplate(@PathVariable("id") UUID id);

    /** Получить шаблоны родителя (админ). */
    @GetMapping("/admin/templates/parent/{parentId}")
    List<TaskTemplateDto> getParentTemplates(@PathVariable("parentId") UUID parentId);

    /** Получить назначения родителя (админ). */
    @GetMapping("/admin/templates/parent/{parentId}/assignments")
    List<TaskAssignmentDto> getParentAssignments(@PathVariable("parentId") UUID parentId);

    /** Получить назначения ребёнка (админ). */
    @GetMapping("/admin/templates/child/{childId}/assignments")
    List<TaskAssignmentDto> getChildAssignments(@PathVariable("childId") UUID childId);

    /** Создать вводное задание для ребёнка. */
    @PostMapping("/assignments/intro/{childId}")
    TaskAssignmentDto createIntroTask(@PathVariable("childId") UUID childId);

    /** Получить группы пресетов из библиотеки заданий. */
    @GetMapping("/admin/templates/library/preset-groups")
    List<String> getTaskPresetGroups();

    /** Применить пресет заданий (для родителя, JWT из контекста). */
    @PostMapping("/templates/library/preset/{group}/apply")
    Map<String, Object> applyTaskPreset(@PathVariable("group") String group);
}
