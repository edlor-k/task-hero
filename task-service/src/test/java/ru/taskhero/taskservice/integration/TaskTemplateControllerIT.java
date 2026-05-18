package ru.taskhero.taskservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.taskhero.taskservice.dto.TaskTemplateCreateRequest;
import ru.taskhero.taskservice.dto.TaskTemplateUpdateRequest;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.repository.TaskTemplateRepository;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("TaskTemplateController Integration Tests")
class TaskTemplateControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskTemplateRepository templateRepository;

    private UUID parentId;

    @BeforeEach
    void setUp() {
        parentId = UUID.randomUUID();
        templateRepository.deleteAll();
    }

    private RequestPostProcessor parentAuth() {
        return authentication(new UsernamePasswordAuthenticationToken(
                parentId, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARENT"))
        ));
    }

    private RequestPostProcessor childAuth() {
        return authentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CHILD"))
        ));
    }

    @Test
    @DisplayName("Должен создать шаблон задания")
    void shouldCreateTemplate() throws Exception {
        // Given
        TaskTemplateCreateRequest request = new TaskTemplateCreateRequest(
                "Убраться в комнате",
                "Навести порядок на столе и пропылесосить",
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
                null,
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/templates")
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Убраться в комнате")))
                .andExpect(jsonPath("$.expReward", is(25)))
                .andExpect(jsonPath("$.coinsReward", is(10)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("Должен вернуть ошибку валидации при пустом названии")
    void shouldReturnValidationErrorForEmptyTitle() throws Exception {
        // Given
        TaskTemplateCreateRequest request = new TaskTemplateCreateRequest(
                "",
                "Описание",
                10,
                5,
                false,
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

        // When & Then
        mockMvc.perform(post("/templates")
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен получить список шаблонов родителя")
    void shouldGetTemplatesByParent() throws Exception {
        // Given
        createTestTemplate("Задание 1");
        createTestTemplate("Задание 2");

        // When & Then
        mockMvc.perform(get("/templates")
                        .with(parentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("Должен получить шаблон по ID")
    void shouldGetTemplateById() throws Exception {
        // Given
        TaskTemplate template = createTestTemplate("Тестовое задание");

        // When & Then
        mockMvc.perform(get("/templates/{id}", template.getId())
                        .with(parentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Тестовое задание")));
    }

    @Test
    @DisplayName("Должен обновить шаблон")
    void shouldUpdateTemplate() throws Exception {
        // Given
        TaskTemplate template = createTestTemplate("Старое название");
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
                null,
                null,
                null
        );

        // When & Then
        mockMvc.perform(put("/templates/{id}", template.getId())
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Новое название")))
                .andExpect(jsonPath("$.expReward", is(30)));
    }

    @Test
    @DisplayName("Должен удалить шаблон")
    void shouldDeleteTemplate() throws Exception {
        // Given
        TaskTemplate template = createTestTemplate("Для удаления");

        // When & Then
        mockMvc.perform(delete("/templates/{id}", template.getId())
                        .with(parentAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Должен вернуть 403 без аутентификации")
    void shouldReturn403WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/templates"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Должен вернуть 403 для ребёнка")
    void shouldReturn403ForChild() throws Exception {
        mockMvc.perform(get("/templates")
                        .with(childAuth()))
                .andExpect(status().isForbidden());
    }

    private TaskTemplate createTestTemplate(String title) {
        TaskTemplate template = TaskTemplate.builder()
                .parentId(parentId)
                .title(title)
                .description("Тестовое описание")
                .expReward(25)
                .coinsReward(10)
                .repeatable(false)
                .active(true)
                .build();
        return templateRepository.save(template);
    }
}
