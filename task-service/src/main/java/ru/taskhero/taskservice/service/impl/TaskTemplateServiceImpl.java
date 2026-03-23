package ru.taskhero.taskservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.aop.LogMethod;
import ru.taskhero.common.model.enums.TaskCategory;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.UnauthorizedException;
import ru.taskhero.taskservice.dto.TaskTemplateCreateRequest;
import ru.taskhero.taskservice.dto.TaskTemplateResponseDto;
import ru.taskhero.taskservice.dto.TaskTemplateUpdateRequest;
import ru.taskhero.taskservice.entity.TaskSubItem;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.mapper.TaskTemplateMapper;
import ru.taskhero.taskservice.repository.TaskTemplateRepository;
import ru.taskhero.taskservice.service.TaskTemplateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Реализация сервиса для управления шаблонами заданий.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TaskTemplateRepository templateRepository;
    private final TaskTemplateMapper templateMapper;

    @Override
    @Transactional
    @LogMethod("template-create")
    public TaskTemplateResponseDto create(UUID parentId, TaskTemplateCreateRequest request) {
        log.info("Создание шаблона задания '{}' для родителя: {}", request.title(), parentId);

        TaskTemplate template = templateMapper.toEntity(request);
        template.setParentId(parentId);

        // Установка значений по умолчанию, если не указаны
        if (request.expReward() != null) {
            template.setExpReward(request.expReward());
        }
        if (request.coinsReward() != null) {
            template.setCoinsReward(request.coinsReward());
        }
        if (request.repeatable() != null) {
            template.setRepeatable(request.repeatable());
        }

        template = templateRepository.save(template);

        // Создание подпунктов
        if (request.subItems() != null && !request.subItems().isEmpty()) {
            List<TaskSubItem> subItems = new ArrayList<>();
            for (int i = 0; i < request.subItems().size(); i++) {
                TaskSubItem subItem = new TaskSubItem();
                subItem.setTemplate(template);
                subItem.setTitle(request.subItems().get(i));
                subItem.setOrderIndex(i);
                subItems.add(subItem);
            }
            template.setSubItems(subItems);
            template = templateRepository.save(template);
        }

        log.info("Шаблон задания создан с ID: {}", template.getId());
        return templateMapper.toDto(template);
    }

    @Override
    @Transactional
    @LogMethod("template-update")
    public TaskTemplateResponseDto update(UUID id, UUID parentId, TaskTemplateUpdateRequest request) {
        log.info("Обновление шаблона {} родителем: {}", id, parentId);

        TaskTemplate template = findTemplateAndVerifyOwnership(id, parentId);

        templateMapper.updateFromRequest(request, template);

        // Обновление подпунктов — заменяем все
        if (request.subItems() != null) {
            template.getSubItems().clear();
            for (int i = 0; i < request.subItems().size(); i++) {
                TaskSubItem subItem = new TaskSubItem();
                subItem.setTemplate(template);
                subItem.setTitle(request.subItems().get(i));
                subItem.setOrderIndex(i);
                template.getSubItems().add(subItem);
            }
        }

        template = templateRepository.save(template);

        log.info("Шаблон {} обновлен", id);
        return templateMapper.toDto(template);
    }

    @Override
    @Transactional
    @LogMethod("template-delete")
    public void delete(UUID id, UUID parentId) {
        log.info("Удаление шаблона {} родителем: {}", id, parentId);

        TaskTemplate template = findTemplateAndVerifyOwnership(id, parentId);
        templateRepository.delete(template);

        log.info("Шаблон {} удален", id);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("template-get-by-id")
    public TaskTemplateResponseDto getById(UUID id) {
        log.debug("Получение шаблона по ID: {}", id);

        TaskTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Шаблон с ID {} не найден", id);
                    return new ResourceNotFoundException("Шаблон с ID " + id + " не найден");
                });

        return templateMapper.toDto(template);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("template-get-by-parent")
    public Page<TaskTemplateResponseDto> getByParent(UUID parentId, Pageable pageable) {
        log.debug("Получение шаблонов родителя: {}, page: {}", parentId, pageable.getPageNumber());

        return templateRepository.findAllByParentId(parentId, pageable)
                .map(templateMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("template-get-active-by-parent")
    public List<TaskTemplateResponseDto> getActiveByParent(UUID parentId) {
        log.debug("Получение активных шаблонов родителя: {}", parentId);

        return templateRepository.findAllByParentIdAndActive(parentId, true)
                .stream()
                .map(templateMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("library-get-all")
    public List<TaskTemplateResponseDto> getLibraryTemplates() {
        log.debug("Получение всех шаблонов библиотеки");
        return templateRepository.findAllByLibraryTemplateTrue()
                .stream()
                .map(templateMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("library-get-by-category")
    public List<TaskTemplateResponseDto> getLibraryTemplatesByCategory(TaskCategory category) {
        log.debug("Получение шаблонов библиотеки по категории: {}", category);
        return templateRepository.findAllByLibraryTemplateTrueAndCategory(category)
                .stream()
                .map(templateMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    @LogMethod("library-copy")
    public TaskTemplateResponseDto copyFromLibrary(UUID libraryTemplateId, UUID parentId) {
        log.info("Копирование шаблона {} из библиотеки для родителя: {}", libraryTemplateId, parentId);

        TaskTemplate libraryTemplate = templateRepository.findById(libraryTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Шаблон библиотеки с ID " + libraryTemplateId + " не найден"));

        if (!libraryTemplate.isLibraryTemplate()) {
            throw new IllegalArgumentException("Шаблон не является шаблоном библиотеки");
        }

        TaskTemplate copy = new TaskTemplate();
        copy.setParentId(parentId);
        copy.setTitle(libraryTemplate.getTitle());
        copy.setDescription(libraryTemplate.getDescription());
        copy.setExpReward(libraryTemplate.getExpReward());
        copy.setCoinsReward(libraryTemplate.getCoinsReward());
        copy.setRepeatable(libraryTemplate.isRepeatable());
        copy.setActive(true);
        copy.setLibraryTemplate(false);
        copy.setCategory(libraryTemplate.getCategory());
        copy.setRecurrenceRule(libraryTemplate.getRecurrenceRule());
        copy.setSourceTemplateId(libraryTemplateId);

        copy = templateRepository.save(copy);

        // Копируем подпункты
        if (libraryTemplate.getSubItems() != null && !libraryTemplate.getSubItems().isEmpty()) {
            List<TaskSubItem> subItems = new ArrayList<>();
            for (TaskSubItem original : libraryTemplate.getSubItems()) {
                TaskSubItem subItem = new TaskSubItem();
                subItem.setTemplate(copy);
                subItem.setTitle(original.getTitle());
                subItem.setOrderIndex(original.getOrderIndex());
                subItems.add(subItem);
            }
            copy.setSubItems(subItems);
            copy = templateRepository.save(copy);
        }

        log.info("Шаблон скопирован из библиотеки, новый ID: {}", copy.getId());
        return templateMapper.toDto(copy);
    }

    @Override
    @Transactional(readOnly = true)
    @LogMethod("library-get-copied-ids")
    public Set<UUID> getCopiedLibraryTemplateIds(UUID parentId) {
        log.debug("Получение ID скопированных шаблонов библиотеки для родителя: {}", parentId);
        return templateRepository.findAllByParentIdAndSourceTemplateIdIsNotNull(parentId)
                .stream()
                .map(TaskTemplate::getSourceTemplateId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    @LogMethod("library-admin-create")
    public TaskTemplateResponseDto createLibraryTemplate(TaskTemplateCreateRequest request) {
        log.info("Админ: создание шаблона библиотеки: {}", request.title());

        TaskTemplate template = templateMapper.toEntity(request);
        template.setLibraryTemplate(true);
        template.setActive(true);

        if (request.expReward() != null) template.setExpReward(request.expReward());
        if (request.coinsReward() != null) template.setCoinsReward(request.coinsReward());
        if (request.repeatable() != null) template.setRepeatable(request.repeatable());

        template = templateRepository.save(template);

        if (request.subItems() != null && !request.subItems().isEmpty()) {
            List<TaskSubItem> subItems = new ArrayList<>();
            for (int i = 0; i < request.subItems().size(); i++) {
                TaskSubItem subItem = new TaskSubItem();
                subItem.setTemplate(template);
                subItem.setTitle(request.subItems().get(i));
                subItem.setOrderIndex(i);
                subItems.add(subItem);
            }
            template.setSubItems(subItems);
            template = templateRepository.save(template);
        }

        log.info("Шаблон библиотеки создан: {}", template.getId());
        return templateMapper.toDto(template);
    }

    @Override
    @Transactional
    @LogMethod("library-admin-update")
    public TaskTemplateResponseDto updateLibraryTemplate(UUID id, TaskTemplateUpdateRequest request) {
        log.info("Админ: обновление шаблона библиотеки: {}", id);

        TaskTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Шаблон библиотеки не найден: " + id));
        if (!template.isLibraryTemplate()) {
            throw new IllegalArgumentException("Шаблон не является шаблоном библиотеки");
        }

        templateMapper.updateFromRequest(request, template);

        if (request.subItems() != null) {
            template.getSubItems().clear();
            for (int i = 0; i < request.subItems().size(); i++) {
                TaskSubItem subItem = new TaskSubItem();
                subItem.setTemplate(template);
                subItem.setTitle(request.subItems().get(i));
                subItem.setOrderIndex(i);
                template.getSubItems().add(subItem);
            }
        }

        template = templateRepository.save(template);
        log.info("Шаблон библиотеки обновлён: {}", id);
        return templateMapper.toDto(template);
    }

    @Override
    @Transactional
    @LogMethod("library-admin-delete")
    public void deleteLibraryTemplate(UUID id) {
        log.info("Админ: удаление шаблона библиотеки: {}", id);

        TaskTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Шаблон библиотеки не найден: " + id));
        if (!template.isLibraryTemplate()) {
            throw new IllegalArgumentException("Шаблон не является шаблоном библиотеки");
        }

        templateRepository.delete(template);
        log.info("Шаблон библиотеки удалён: {}", id);
    }

    /**
     * Найти шаблон и проверить, что он принадлежит указанному родителю.
     *
     * @param id       ID шаблона
     * @param parentId ID родителя
     * @return шаблон
     * @throws ResourceNotFoundException если шаблон не найден
     * @throws UnauthorizedException     если шаблон не принадлежит родителю
     */
    private TaskTemplate findTemplateAndVerifyOwnership(UUID id, UUID parentId) {
        TaskTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Шаблон с ID {} не найден", id);
                    return new ResourceNotFoundException("Шаблон с ID " + id + " не найден");
                });

        if (!template.getParentId().equals(parentId)) {
            log.warn("Родитель {} пытается получить доступ к шаблону {} другого родителя", parentId, id);
            throw new UnauthorizedException("Вы не можете управлять этим шаблоном");
        }

        return template;
    }
}
