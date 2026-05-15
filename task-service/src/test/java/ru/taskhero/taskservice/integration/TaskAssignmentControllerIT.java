package ru.taskhero.taskservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.client.UserServiceClient;
import ru.taskhero.taskservice.dto.TaskAssignRequest;
import ru.taskhero.taskservice.dto.TaskReviewRequest;
import ru.taskhero.taskservice.dto.TaskSubmitRequest;
import ru.taskhero.taskservice.entity.TaskAssignment;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.repository.TaskAssignmentRepository;
import ru.taskhero.taskservice.repository.TaskTemplateRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("TaskAssignmentController Integration Tests")
class TaskAssignmentControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskTemplateRepository templateRepository;

    @Autowired
    private TaskAssignmentRepository assignmentRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    private UUID parentId;
    private UUID childId;
    private TaskTemplate template;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        templateRepository.deleteAll();

        parentId = UUID.randomUUID();
        childId = UUID.randomUUID();

        template = templateRepository.save(TaskTemplate.builder()
                .parentId(parentId)
                .title("Помыть посуду")
                .description("Вымыть всю посуду после ужина")
                .expReward(20)
                .coinsReward(8)
                .repeatable(false)
                .active(true)
                .build());

        when(userServiceClient.addReward(any(), any())).thenReturn(null);
    }

    private RequestPostProcessor parentAuth() {
        return authentication(new UsernamePasswordAuthenticationToken(
                parentId, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARENT"))
        ));
    }

    private RequestPostProcessor parentAuth(UUID id) {
        return authentication(new UsernamePasswordAuthenticationToken(
                id, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARENT"))
        ));
    }

    private RequestPostProcessor childAuth() {
        return authentication(new UsernamePasswordAuthenticationToken(
                childId, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CHILD"))
        ));
    }

    @Test
    @DisplayName("Должен назначить задание ребёнку")
    void shouldAssignTaskToChild() throws Exception {
        // Given
        TaskAssignRequest request = new TaskAssignRequest(
                template.getId(), childId,
                Instant.now().plus(Duration.ofDays(7)), null
        );

        // When & Then
        mockMvc.perform(post("/assignments")
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.childId", is(childId.toString())))
                .andExpect(jsonPath("$.status", is("CREATED")));
    }

    @Test
    @DisplayName("Должен вернуть 403 при назначении чужого шаблона")
    void shouldReturn403WhenAssigningOthersTemplate() throws Exception {
        // Given
        UUID otherParentId = UUID.randomUUID();
        TaskAssignRequest request = new TaskAssignRequest(template.getId(), childId, null, null);

        // When & Then
        mockMvc.perform(post("/assignments")
                        .with(parentAuth(otherParentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Должен вернуть 403 без аутентификации")
    void shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Должен сдать задание на проверку")
    void shouldSubmitAssignment() throws Exception {
        // Given
        TaskAssignment assignment = assignmentRepository.save(TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.CREATED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build());

        TaskSubmitRequest request = new TaskSubmitRequest("Всё готово!");

        // When & Then
        mockMvc.perform(post("/assignments/{id}/submit", assignment.getId())
                        .with(childAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")));
    }

    @Test
    @DisplayName("Должен одобрить задание и начислить награду")
    void shouldApproveAssignment() throws Exception {
        // Given
        TaskAssignment assignment = assignmentRepository.save(TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.SUBMITTED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build());

        TaskReviewRequest request = new TaskReviewRequest("Отлично!", null, null);

        // When & Then
        mockMvc.perform(post("/assignments/{id}/approve", assignment.getId())
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    @DisplayName("Должен отклонить задание")
    void shouldRejectAssignment() throws Exception {
        // Given
        TaskAssignment assignment = assignmentRepository.save(TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.SUBMITTED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build());

        TaskReviewRequest request = new TaskReviewRequest("Нужно переделать", null, null);

        // When & Then
        mockMvc.perform(post("/assignments/{id}/reject", assignment.getId())
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    @Test
    @DisplayName("Должен вернуть задания ребёнка")
    void shouldGetAssignmentsByChild() throws Exception {
        // Given
        assignmentRepository.save(TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.CREATED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build());

        // When & Then
        mockMvc.perform(get("/assignments/child/{childId}", childId)
                        .with(parentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    @DisplayName("Должен вернуть задания на проверку для родителя")
    void shouldGetPendingReviewForParent() throws Exception {
        // Given
        assignmentRepository.save(TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.SUBMITTED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build());

        // When & Then
        mockMvc.perform(get("/assignments/pending-review")
                        .with(parentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    @DisplayName("Должен вернуть 400 при одобрении не-сданного задания")
    void shouldReturn400WhenApprovingNonSubmittedTask() throws Exception {
        // Given — задание ещё в статусе CREATED
        TaskAssignment assignment = assignmentRepository.save(TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.CREATED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build());

        TaskReviewRequest request = new TaskReviewRequest(null, null, null);

        // When & Then
        mockMvc.perform(post("/assignments/{id}/approve", assignment.getId())
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
