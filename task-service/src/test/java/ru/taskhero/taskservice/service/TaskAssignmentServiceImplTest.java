package ru.taskhero.taskservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.UnauthorizedException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.dto.TaskAssignRequest;
import ru.taskhero.taskservice.dto.TaskAssignmentResponseDto;
import ru.taskhero.taskservice.dto.TaskReviewRequest;
import ru.taskhero.taskservice.dto.TaskSubmitRequest;
import ru.taskhero.taskservice.dto.TaskTemplateResponseDto;
import ru.taskhero.taskservice.entity.TaskAssignment;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.mapper.TaskAssignmentMapper;
import ru.taskhero.taskservice.repository.TaskAssignmentRepository;
import ru.taskhero.taskservice.repository.TaskTemplateRepository;
import ru.taskhero.taskservice.service.impl.TaskAssignmentServiceImpl;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskAssignmentService Unit Tests")
class TaskAssignmentServiceImplTest {

    @Mock
    private TaskAssignmentRepository assignmentRepository;

    @Mock
    private TaskTemplateRepository templateRepository;

    @Mock
    private TaskAssignmentMapper assignmentMapper;

    @Mock
    private RewardService rewardService;

    @InjectMocks
    private TaskAssignmentServiceImpl assignmentService;

    private UUID parentId;
    private UUID childId;
    private UUID templateId;
    private UUID assignmentId;
    private TaskTemplate template;
    private TaskAssignment assignment;
    private TaskAssignmentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        parentId = UUID.randomUUID();
        childId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();

        template = TaskTemplate.builder()
                .parentId(parentId)
                .title("Убраться в комнате")
                .expReward(25)
                .coinsReward(10)
                .active(true)
                .repeatable(false)
                .build();

        assignment = TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.CREATED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build();

        TaskTemplateResponseDto templateDto = new TaskTemplateResponseDto(
                templateId, parentId, "Убраться в комнате", null,
                25, 10, false, null, null, null, null,
                null, null, null, true, false, null, null, Instant.now(), null
        );

        responseDto = new TaskAssignmentResponseDto(
                assignmentId, childId, TaskStatus.CREATED, Instant.now().plus(Duration.ofDays(7)),
                null, null, null, null, null, null,
                null, null, templateDto, Instant.now(), null
        );
    }

    @Test
    @DisplayName("Должен назначить задание ребёнку")
    void shouldAssignTaskToChild() {
        // Given
        TaskAssignRequest request = new TaskAssignRequest(
                templateId, childId, Instant.now().plus(Duration.ofDays(7)), null
        );

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(assignmentRepository.existsByChildIdAndTemplateIdAndStatusIn(eq(childId), eq(templateId), any()))
                .thenReturn(false);
        when(assignmentRepository.save(any(TaskAssignment.class))).thenReturn(assignment);
        when(assignmentMapper.toDto(assignment)).thenReturn(responseDto);

        // When
        TaskAssignmentResponseDto result = assignmentService.assign(parentId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.childId()).isEqualTo(childId);
        assertThat(result.status()).isEqualTo(TaskStatus.CREATED);
        verify(assignmentRepository).save(any(TaskAssignment.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при назначении чужого шаблона")
    void shouldThrowExceptionWhenAssigningOthersTemplate() {
        // Given
        UUID otherParentId = UUID.randomUUID();
        TaskAssignRequest request = new TaskAssignRequest(templateId, childId, null, null);

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When & Then
        assertThatThrownBy(() -> assignmentService.assign(otherParentId, request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Должен выбросить исключение при повторном назначении не-повторяемого задания")
    void shouldThrowExceptionWhenAssigningDuplicateNonRepeatable() {
        // Given
        TaskAssignRequest request = new TaskAssignRequest(templateId, childId, null, null);

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(assignmentRepository.existsByChildIdAndTemplateIdAndStatusIn(eq(childId), eq(templateId), any()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> assignmentService.assign(parentId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("уже есть активное задание");
    }

    @Test
    @DisplayName("Должен сдать задание на проверку")
    void shouldSubmitTask() {
        // Given
        TaskSubmitRequest request = new TaskSubmitRequest("Я все сделал!");

        when(assignmentRepository.findByIdWithTemplate(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(TaskAssignment.class))).thenReturn(assignment);
        when(assignmentMapper.toDto(assignment)).thenReturn(
                new TaskAssignmentResponseDto(
                        assignmentId, childId, TaskStatus.SUBMITTED, null,
                        "Я все сделал!", null, Instant.now(), null, null, null,
                        null, null, null, Instant.now(), null
                )
        );

        // When
        TaskAssignmentResponseDto result = assignmentService.submit(assignmentId, childId, request);

        // Then
        assertThat(result).isNotNull();
        verify(assignmentRepository).save(any(TaskAssignment.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при сдаче чужого задания")
    void shouldThrowExceptionWhenSubmittingOthersTask() {
        // Given
        UUID otherChildId = UUID.randomUUID();
        TaskSubmitRequest request = new TaskSubmitRequest(null);

        when(assignmentRepository.findByIdWithTemplate(assignmentId)).thenReturn(Optional.of(assignment));

        // When & Then
        assertThatThrownBy(() -> assignmentService.submit(assignmentId, otherChildId, request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("не ваше задание");
    }

    @Test
    @DisplayName("Должен одобрить задание и начислить награду")
    void shouldApproveTaskAndGrantReward() {
        // Given
        assignment.setStatus(TaskStatus.SUBMITTED);
        TaskReviewRequest request = new TaskReviewRequest("Молодец!", null, null);

        when(assignmentRepository.findByIdWithTemplate(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(TaskAssignment.class))).thenReturn(assignment);
        when(assignmentMapper.toDto(assignment)).thenReturn(
                new TaskAssignmentResponseDto(
                        assignmentId, childId, TaskStatus.APPROVED, null,
                        null, "Молодец!", null, Instant.now(), 25, 10,
                        null, null, null, Instant.now(), Instant.now()
                )
        );
        when(assignmentRepository.existsByChildIdAndImportantTrueAndStatusIn(any(), any()))
                .thenReturn(false);

        // When
        TaskAssignmentResponseDto result = assignmentService.approve(assignmentId, parentId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TaskStatus.APPROVED);
        verify(rewardService).grantReward(childId, 25, 10, false);
    }

    @Test
    @DisplayName("Должен отклонить задание без начисления награды")
    void shouldRejectTaskWithoutReward() {
        // Given
        assignment.setStatus(TaskStatus.SUBMITTED);
        TaskReviewRequest request = new TaskReviewRequest("Нужно переделать", null, null);

        when(assignmentRepository.findByIdWithTemplate(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(TaskAssignment.class))).thenReturn(assignment);
        when(assignmentMapper.toDto(assignment)).thenReturn(
                new TaskAssignmentResponseDto(
                        assignmentId, childId, TaskStatus.REJECTED, null,
                        null, "Нужно переделать", null, Instant.now(), null, null,
                        null, null, null, Instant.now(), Instant.now()
                )
        );

        // When
        TaskAssignmentResponseDto result = assignmentService.reject(assignmentId, parentId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TaskStatus.REJECTED);
    }

    @Test
    @DisplayName("Должен выбросить исключение при одобрении задания не в статусе SUBMITTED")
    void shouldThrowExceptionWhenApprovingNonSubmittedTask() {
        // Given
        assignment.setStatus(TaskStatus.CREATED);
        TaskReviewRequest request = new TaskReviewRequest(null, null, null);

        when(assignmentRepository.findByIdWithTemplate(assignmentId)).thenReturn(Optional.of(assignment));

        // When & Then
        assertThatThrownBy(() -> assignmentService.approve(assignmentId, parentId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("не находится на проверке");
    }

    @Test
    @DisplayName("Должен получить назначения ребёнка")
    void shouldGetAssignmentsByChild() {
        // Given
        when(assignmentRepository.findAllByChildIdWithTemplate(childId)).thenReturn(List.of(assignment));
        when(assignmentMapper.toDto(assignment)).thenReturn(responseDto);

        // When
        List<TaskAssignmentResponseDto> result = assignmentService.getByChild(childId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).childId()).isEqualTo(childId);
    }

    @Test
    @DisplayName("Должен получить активные назначения ребёнка")
    void shouldGetActiveAssignmentsByChild() {
        // Given
        List<TaskStatus> activeStatuses = List.of(TaskStatus.CREATED, TaskStatus.SUBMITTED);
        when(assignmentRepository.findAllByChildIdAndStatusInWithTemplate(childId, activeStatuses))
                .thenReturn(List.of(assignment));
        when(assignmentMapper.toDto(assignment)).thenReturn(responseDto);

        // When
        List<TaskAssignmentResponseDto> result = assignmentService.getActiveByChild(childId);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Должен получить назначения на проверку для родителя")
    void shouldGetPendingReviewByParent() {
        // Given
        assignment.setStatus(TaskStatus.SUBMITTED);
        when(assignmentRepository.findAllByParentIdAndStatusWithTemplate(parentId, TaskStatus.SUBMITTED))
                .thenReturn(List.of(assignment));
        when(assignmentMapper.toDto(assignment)).thenReturn(responseDto);

        // When
        List<TaskAssignmentResponseDto> result = assignmentService.getPendingReviewByParent(parentId);

        // Then
        assertThat(result).hasSize(1);
    }
}
