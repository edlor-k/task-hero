package ru.taskhero.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.UserLoginRequestDto;
import ru.taskhero.userservice.dto.UserRegisterRequest;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.repository.UserRepository;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController Integration Tests")
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Должен зарегистрировать нового родителя")
    void shouldRegisterNewParent() throws Exception {
        // Given
        UserRegisterRequest request = new UserRegisterRequest(
                "newparent@example.com", "password123", "Иван", "Петров"
        );

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("newparent@example.com")))
                .andExpect(jsonPath("$.role", is("PARENT")))
                .andExpect(jsonPath("$.parentId", notNullValue()))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @DisplayName("Должен вернуть 400 при регистрации с существующим email")
    void shouldReturn400ForDuplicateEmail() throws Exception {
        // Given
        User existing = User.builder()
                .email("existing@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.PARENT)
                .active(true)
                .build();
        userRepository.save(existing);

        UserRegisterRequest request = new UserRegisterRequest(
                "existing@example.com", "password456", "Другой", "Пользователь"
        );

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен вернуть 400 при некорректном email при регистрации")
    void shouldReturn400ForInvalidEmail() throws Exception {
        // Given
        UserRegisterRequest request = new UserRegisterRequest(
                "not-an-email", "password123", "Иван", "Петров"
        );

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен выдать JWT токен при успешном входе")
    void shouldReturnJwtTokenOnLogin() throws Exception {
        // Given
        User user = User.builder()
                .email("parent@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.PARENT)
                .active(true)
                .build();
        userRepository.save(user);

        UserLoginRequestDto request = new UserLoginRequestDto("parent@example.com", "password123");

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.displayName", is("parent@example.com")))
                .andExpect(jsonPath("$.role", is("PARENT")));
    }

    @Test
    @DisplayName("Должен вернуть 401 при неверном пароле")
    void shouldReturn401ForWrongPassword() throws Exception {
        // Given
        User user = User.builder()
                .email("parent@example.com")
                .password(passwordEncoder.encode("correct_password"))
                .role(Role.PARENT)
                .active(true)
                .build();
        userRepository.save(user);

        UserLoginRequestDto request = new UserLoginRequestDto("parent@example.com", "wrong_password");

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Должен вернуть 401 для заблокированного пользователя")
    void shouldReturn401ForBlockedUser() throws Exception {
        // Given
        User blocked = User.builder()
                .email("blocked@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.PARENT)
                .active(false)
                .build();
        userRepository.save(blocked);

        UserLoginRequestDto request = new UserLoginRequestDto("blocked@example.com", "password123");

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Должен вернуть 400 при слишком коротком пароле")
    void shouldReturn400ForShortPassword() throws Exception {
        // Given
        UserRegisterRequest request = new UserRegisterRequest(
                "new@example.com", "12345", "Иван", "Петров"
        );

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
