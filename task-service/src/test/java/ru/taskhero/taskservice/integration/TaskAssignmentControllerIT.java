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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.taskhero.common.model.enums.TaskStatus;
import ru.taskhero.taskservice.client.UserServiceClient;
import ru.taskhero.taskservice.dto.AssignTaskRequest;
import ru.taskhero.taskservice.dto.RewardRequest;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("Не должен начислить награду дважды при повторном одобрении (защита от двойного клика)")
    void shouldNotDoubleGrantRewardOnRepeatedApprove() throws Exception {
        // Given
        TaskAssignment assignment = assignmentRepository.save(TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.SUBMITTED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build());

        TaskReviewRequest request = new TaskReviewRequest("Отлично!", null, null);

        // When — первый запрос одобряет и начисляет награду
        mockMvc.perform(post("/assignments/{id}/approve", assignment.getId())
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Then — повторный запрос на то же задание должен быть отклонён без повторного начисления
        mockMvc.perform(post("/assignments/{id}/approve", assignment.getId())
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verify(userServiceClient, org.mockito.Mockito.times(1)).addReward(any(), any());
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

    @Test
    @DisplayName("Полный цикл: назначено -> на проверке -> подтверждено, с корректной наградой и ID назначения")
    void shouldGoThroughFullLifecycleAssignSubmitApprove() throws Exception {
        // assign
        TaskAssignRequest assignRequest = new TaskAssignRequest(
                template.getId(), childId, Instant.now().plus(Duration.ofDays(7)), null);
        String assignResponse = mockMvc.perform(post("/assignments")
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andReturn().getResponse().getContentAsString();
        UUID assignmentId = UUID.fromString(objectMapper.readTree(assignResponse).get("id").asText());

        // submit
        mockMvc.perform(post("/assignments/{id}/submit", assignmentId)
                        .with(childAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskSubmitRequest("Готово"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")));

        // approve
        mockMvc.perform(post("/assignments/{id}/approve", assignmentId)
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskReviewRequest(null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.expEarned", is(20)))
                .andExpect(jsonPath("$.coinsEarned", is(8)));

        org.mockito.ArgumentCaptor<RewardRequest> captor = org.mockito.ArgumentCaptor.forClass(RewardRequest.class);
        org.mockito.Mockito.verify(userServiceClient).addReward(eq(childId), captor.capture());
        assertThat(captor.getValue().sourceAssignmentId()).isEqualTo(assignmentId);
        assertThat(captor.getValue().exp()).isEqualTo(20);
        assertThat(captor.getValue().coins()).isEqualTo(8);
    }

    @Test
    @DisplayName("Отклонённое задание — терминальное состояние: повторная сдача отклоняется")
    void rejectedAssignmentShouldBeTerminal() throws Exception {
        // Given
        TaskAssignment assignment = assignmentRepository.save(TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.SUBMITTED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build());

        mockMvc.perform(post("/assignments/{id}/reject", assignment.getId())
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskReviewRequest("Переделай", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        // When & Then — повторная сдача отклонённого задания не предусмотрена продуктом
        mockMvc.perform(post("/assignments/{id}/submit", assignment.getId())
                        .with(childAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskSubmitRequest(null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("assign-with-template: должен атомарно создать шаблон и назначение (валидный дедлайн)")
    void shouldAtomicallyCreateTemplateAndAssignWithValidDeadline() throws Exception {
        // Given
        long templatesBefore = templateRepository.count();
        AssignTaskRequest request = new AssignTaskRequest(
                null, childId, Instant.now().plus(Duration.ofDays(3)), null,
                "Прибраться в комнате", "Разложить вещи по местам", 7, null);

        // When & Then
        mockMvc.perform(post("/assignments/assign-with-template")
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andExpect(jsonPath("$.template.title", is("Прибраться в комнате")));

        assertThat(templateRepository.count()).isEqualTo(templatesBefore + 1);
    }

    @Test
    @DisplayName("assign-with-template: невалидные данные не должны создавать шаблон-сироту (нет частичного сохранения)")
    void shouldNotLeaveOrphanTemplateWhenAssignWithTemplateFails() throws Exception {
        // Given — templateId не передан, а childId вообще отсутствует: запрос не пройдёт валидацию
        long templatesBefore = templateRepository.count();
        String invalidJson = """
                {"childId": null, "title": "Задание без ребёнка"}
                """;

        // When & Then
        mockMvc.perform(post("/assignments/assign-with-template")
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        // Then — ни шаблон, ни назначение не сохранены
        assertThat(templateRepository.count()).isEqualTo(templatesBefore);
    }

    @Test
    @DisplayName("assign-with-template: ошибка бизнес-валидации (пустое название) не должна создавать шаблон-сироту")
    void shouldNotLeaveOrphanTemplateWhenTitleBlank() throws Exception {
        // Given
        long templatesBefore = templateRepository.count();
        AssignTaskRequest request = new AssignTaskRequest(
                null, childId, null, null, null, null, 5, null);

        // When & Then
        mockMvc.perform(post("/assignments/assign-with-template")
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(templateRepository.count()).isEqualTo(templatesBefore);
    }

    @Test
    @DisplayName("Два конкурентных запроса на одобрение одного задания — награда начисляется ровно один раз")
    void shouldHandleConcurrentApproveRequestsIdempotently() throws Exception {
        // Given
        TaskAssignment assignment = assignmentRepository.save(TaskAssignment.builder()
                .template(template)
                .childId(childId)
                .status(TaskStatus.SUBMITTED)
                .dueDate(Instant.now().plus(Duration.ofDays(7)))
                .build());
        UUID assignmentId = assignment.getId();
        String body = objectMapper.writeValueAsString(new TaskReviewRequest(null, null, null));

        // Тестовый метод обёрнут в единую транзакцию (@Transactional на AbstractIntegrationTest).
        // Настоящая проверка PESSIMISTIC_WRITE-блокировки требует двух РЕАЛЬНЫХ параллельных
        // транзакций/соединений — выходим из тестовой транзакции, зафиксировав setUp().
        TestTransaction.flagForCommit();
        TestTransaction.end();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        Callable<Integer> approveCall = () -> {
            ready.countDown();
            go.await();
            return mockMvc.perform(post("/assignments/{id}/approve", assignmentId)
                            .with(parentAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();
        };

        try {
            Future<Integer> f1 = pool.submit(approveCall);
            Future<Integer> f2 = pool.submit(approveCall);
            ready.await();
            go.countDown();

            int status1 = f1.get(10, TimeUnit.SECONDS);
            int status2 = f2.get(10, TimeUnit.SECONDS);

            // Один запрос выигрывает гонку и одобряет (200), второй видит уже APPROVED и
            // отклоняется (400) — именно так и должна вести себя пессимистичная блокировка.
            assertThat(List.of(status1, status2)).containsExactlyInAnyOrder(200, 400);
        } finally {
            pool.shutdown();
        }

        org.mockito.Mockito.verify(userServiceClient, org.mockito.Mockito.times(1)).addReward(any(), any());

        // Восстанавливаем транзакцию, чтобы штатный rollback-после-теста отработал как обычно.
        TestTransaction.start();
    }
}
