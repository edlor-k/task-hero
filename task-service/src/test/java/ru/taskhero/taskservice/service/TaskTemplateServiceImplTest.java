package ru.taskhero.taskservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.UnauthorizedException;
import ru.taskhero.taskservice.dto.TaskTemplateCreateRequest;
import ru.taskhero.taskservice.dto.TaskTemplateResponseDto;
import ru.taskhero.taskservice.dto.TaskTemplateUpdateRequest;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.mapper.TaskTemplateMapper;
import ru.taskhero.taskservice.repository.TaskTemplateRepository;
import ru.taskhero.taskservice.service.impl.TaskTemplateServiceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskTemplateService Unit Tests")
class TaskTemplateServiceImplTest {

    @Mock
    private TaskTemplateRepository templateRepository;

    @Mock
    private TaskTemplateMapper templateMapper;

    @InjectMocks
    private TaskTemplateServiceImpl templateService;

    private UUID parentId;
    private UUID templateId;
    private TaskTemplate template;
    private TaskTemplateResponseDto responseDto;

    @BeforeEach
    void setUp() {
        parentId = UUID.randomUUID();
        templateId = UUID.randomUUID();

        template = TaskTemplate.builder()
                .parentId(parentId)
                .title("Убраться в комнате")
                .description("Навести порядок")
                .expReward(25)
                .coinsReward(10)
                .repeatable(false)
                .active(true)
                .build();

        responseDto = new TaskTemplateResponseDto(
                templateId,
                parentId,
                "Убраться в комнате",
                "Навести порядок",
                25,
                10,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                null,
                Instant.now(),
                null
        );
    }

    @Test
    @DisplayName("Должен создать шаблон задания")
    void shouldCreateTemplate() {
        // Given
        TaskTemplateCreateRequest request = new TaskTemplateCreateRequest(
                "Убраться в комнате",
                "Навести порядок",
                25,
                10,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(templateMapper.toEntity(request)).thenReturn(template);
        when(templateRepository.save(any(TaskTemplate.class))).thenReturn(template);
        when(templateMapper.toDto(template)).thenReturn(responseDto);

        // When
        TaskTemplateResponseDto result = templateService.create(parentId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Убраться в комнате");
        assertThat(result.expReward()).isEqualTo(25);
        verify(templateRepository).save(any(TaskTemplate.class));
    }

    @Test
    @DisplayName("Должен получить шаблон по ID")
    void shouldGetTemplateById() {
        // Given
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(templateMapper.toDto(template)).thenReturn(responseDto);

        // When
        TaskTemplateResponseDto result = templateService.getById(templateId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(templateId);
    }

    @Test
    @DisplayName("Должен выбросить исключение при получении несуществующего шаблона")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Given
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> templateService.getById(templateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("Должен получить шаблоны родителя с пагинацией")
    void shouldGetTemplatesByParent() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskTemplate> page = new PageImpl<>(List.of(template));

        when(templateRepository.findAllByParentId(parentId, pageable)).thenReturn(page);
        when(templateMapper.toDto(template)).thenReturn(responseDto);

        // When
        Page<TaskTemplateResponseDto> result = templateService.getByParent(parentId, pageable);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Должен обновить шаблон")
    void shouldUpdateTemplate() {
        // Given
        TaskTemplateUpdateRequest request = new TaskTemplateUpdateRequest(
                "Новое название",
                null,
                30,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(template)).thenReturn(template);
        when(templateMapper.toDto(template)).thenReturn(responseDto);

        // When
        TaskTemplateResponseDto result = templateService.update(templateId, parentId, request);

        // Then
        assertThat(result).isNotNull();
        verify(templateMapper).updateFromRequest(request, template);
        verify(templateRepository).save(template);
    }

    @Test
    @DisplayName("Должен выбросить исключение при обновлении чужого шаблона")
    void shouldThrowExceptionWhenUpdatingOthersTemplate() {
        // Given
        UUID otherParentId = UUID.randomUUID();
        TaskTemplateUpdateRequest request = new TaskTemplateUpdateRequest(
                "Новое название", null, null, null, null, null, null,
                null, null, null, null, null, null, null
        );

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When & Then
        assertThatThrownBy(() -> templateService.update(templateId, otherParentId, request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Должен удалить шаблон")
    void shouldDeleteTemplate() {
        // Given
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        templateService.delete(templateId, parentId);

        // Then
        verify(templateRepository).delete(template);
    }

    @Test
    @DisplayName("Должен получить активные шаблоны родителя")
    void shouldGetActiveTemplatesByParent() {
        // Given
        when(templateRepository.findAllByParentIdAndActive(parentId, true)).thenReturn(List.of(template));
        when(templateMapper.toDto(template)).thenReturn(responseDto);

        // When
        List<TaskTemplateResponseDto> result = templateService.getActiveByParent(parentId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).active()).isTrue();
    }
}
