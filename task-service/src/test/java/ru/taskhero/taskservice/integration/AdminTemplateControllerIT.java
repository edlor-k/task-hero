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
import ru.taskhero.taskservice.client.UserServiceClient;
import ru.taskhero.taskservice.dto.TaskTemplateUpdateRequest;
import ru.taskhero.taskservice.entity.TaskTemplate;
import ru.taskhero.taskservice.repository.TaskAssignmentRepository;
import ru.taskhero.taskservice.repository.TaskTemplateRepository;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminTemplateController Integration Tests")
class AdminTemplateControllerIT extends AbstractIntegrationTest {

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

    private UUID adminId;
    private UUID parentId;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        templateRepository.deleteAll();
        adminId = UUID.randomUUID();
        parentId = UUID.randomUUID();
    }

    private RequestPostProcessor adminAuth() {
        return authentication(new UsernamePasswordAuthenticationToken(
                adminId, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
    }

    private RequestPostProcessor parentAuth() {
        return authentication(new UsernamePasswordAuthenticationToken(
                parentId, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARENT"))
        ));
    }

    private TaskTemplate saveLibraryTemplate(String title) {
        return templateRepository.save(TaskTemplate.builder()
                .parentId(UUID.randomUUID()) // system UUID for library templates
                .title(title)
                .description("Описание")
                .expReward(10)
                .coinsReward(5)
                .repeatable(false)
                .active(true)
                .libraryTemplate(true)
                .build());
    }

    @Test
    @DisplayName("Должен вернуть список шаблонов библиотеки")
    void shouldGetLibraryTemplates() throws Exception {
        // Given
        saveLibraryTemplate("Шаблон библиотеки");

        // When & Then
        mockMvc.perform(get("/admin/templates/library")
                        .with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].libraryTemplate", is(true)));
    }

    @Test
    @DisplayName("Должен обновить шаблон библиотеки")
    void shouldUpdateLibraryTemplate() throws Exception {
        // Given
        TaskTemplate template = saveLibraryTemplate("Старый заголовок");
        TaskTemplateUpdateRequest update = new TaskTemplateUpdateRequest(
                "Новый заголовок", null, 20, null, null, null,
                null, null, null, null, null, null, null, null, null, null
        );

        // When & Then
        mockMvc.perform(put("/admin/templates/library/{id}", template.getId())
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Новый заголовок")))
                .andExpect(jsonPath("$.expReward", is(20)));
    }

    @Test
    @DisplayName("Должен удалить шаблон библиотеки")
    void shouldDeleteLibraryTemplate() throws Exception {
        // Given
        TaskTemplate template = saveLibraryTemplate("Для удаления");

        // When & Then
        mockMvc.perform(delete("/admin/templates/library/{id}", template.getId())
                        .with(adminAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Должен вернуть шаблоны конкретного родителя")
    void shouldGetParentTemplates() throws Exception {
        // Given
        templateRepository.save(TaskTemplate.builder()
                .parentId(parentId)
                .title("Шаблон родителя")
                .description("Описание")
                .expReward(10)
                .coinsReward(5)
                .repeatable(false)
                .active(true)
                .build());

        // When & Then
        mockMvc.perform(get("/admin/templates/parent/{parentId}", parentId)
                        .with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Должен вернуть пустой список если у родителя нет шаблонов")
    void shouldReturnEmptyListForParentWithNoTemplates() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/templates/parent/{parentId}", UUID.randomUUID())
                        .with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Должен вернуть 403 для не-ADMIN пользователя")
    void shouldReturn403ForNonAdmin() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/templates/library")
                        .with(parentAuth()))
                .andExpect(status().isForbidden());
    }
}
