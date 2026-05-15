package ru.taskhero.userservice.integration;

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
import ru.taskhero.common.model.enums.DifficultyTrajectory;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.ChildCreateRequestDto;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.Parent;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.ParentRepository;
import ru.taskhero.userservice.repository.UserRepository;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ChildController Integration Tests")
class ChildControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ChildRepository childRepository;

    private UUID userId;
    private UUID parentId;

    @BeforeEach
    void setUp() {
        childRepository.deleteAll();
        parentRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .email("parent@example.com")
                .password("hashed_pass")
                .role(Role.PARENT)
                .active(true)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Parent parent = Parent.builder()
                .user(user)
                .firstName("Иван")
                .surname("Петров")
                .build();
        parent = parentRepository.save(parent);
        parentId = parent.getId();
    }

    private RequestPostProcessor parentAuth() {
        return authentication(new UsernamePasswordAuthenticationToken(
                userId, null,
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
    @DisplayName("Должен создать ребёнка для авторизованного родителя")
    void shouldCreateChildForParent() throws Exception {
        // Given
        ChildCreateRequestDto request = new ChildCreateRequestDto("Алиса", "Петрова", DifficultyTrajectory.NORMAL);

        // When & Then
        mockMvc.perform(post("/children")
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is("Алиса")))
                .andExpect(jsonPath("$.loginToken", notNullValue()))
                .andExpect(jsonPath("$.level", is(1)))
                .andExpect(jsonPath("$.exp", is(0)));
    }

    @Test
    @DisplayName("Должен вернуть 403 при создании ребёнка без роли PARENT")
    void shouldReturn403ForChildRole() throws Exception {
        // Given
        ChildCreateRequestDto request = new ChildCreateRequestDto("Боб", "Иванов", null);

        // When & Then
        mockMvc.perform(post("/children")
                        .with(childAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Должен вернуть 403 без аутентификации")
    void shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(post("/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Должен вернуть список детей родителя")
    void shouldGetChildrenForParent() throws Exception {
        // Given
        Parent parent = parentRepository.findById(parentId).orElseThrow();
        Child child1 = Child.builder()
                .parent(parent).firstName("Алиса").surname("Петрова")
                .loginToken("TOKEN-A").exp(0).coins(0).level(1)
                .difficultyTrajectory(DifficultyTrajectory.NORMAL).build();
        Child child2 = Child.builder()
                .parent(parent).firstName("Боб").surname("Петров")
                .loginToken("TOKEN-B").exp(0).coins(0).level(1)
                .difficultyTrajectory(DifficultyTrajectory.NORMAL).build();
        childRepository.save(child1);
        childRepository.save(child2);

        // When & Then
        mockMvc.perform(get("/children")
                        .with(parentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    @DisplayName("Должен получить ребёнка по ID")
    void shouldGetChildById() throws Exception {
        // Given
        Parent parent = parentRepository.findById(parentId).orElseThrow();
        Child child = Child.builder()
                .parent(parent).firstName("Алиса").surname("Петрова")
                .loginToken("TOKEN-ABC").exp(0).coins(0).level(1)
                .difficultyTrajectory(DifficultyTrajectory.NORMAL).build();
        child = childRepository.save(child);
        UUID childId = child.getId();

        // When & Then
        mockMvc.perform(get("/children/{id}", childId)
                        .with(parentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(childId.toString())))
                .andExpect(jsonPath("$.firstName", is("Алиса")));
    }

    @Test
    @DisplayName("Должен вернуть 404 для несуществующего ребёнка")
    void shouldReturn404ForUnknownChild() throws Exception {
        // Given
        UUID unknownId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/children/{id}", unknownId)
                        .with(parentAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Должен удалить ребёнка")
    void shouldDeleteChild() throws Exception {
        // Given
        Parent parent = parentRepository.findById(parentId).orElseThrow();
        Child child = Child.builder()
                .parent(parent).firstName("Для удаления").surname("Тест")
                .loginToken("TOKEN-DEL").exp(0).coins(0).level(1)
                .difficultyTrajectory(DifficultyTrajectory.NORMAL).build();
        child = childRepository.save(child);
        UUID childId = child.getId();

        // When & Then
        mockMvc.perform(delete("/children/{id}", childId)
                        .with(parentAuth()))
                .andExpect(status().isNoContent());
    }
}
