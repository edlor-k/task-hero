package ru.taskhero.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.taskhero.common.model.enums.DifficultyTrajectory;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.RewardRequest;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.Parent;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.ParentRepository;
import ru.taskhero.userservice.repository.RewardGrantRepository;
import ru.taskhero.userservice.repository.UserRepository;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Доказывает, что уникальное ограничение БД на {@code reward_grants.source_assignment_id}
 * реально защищает от повторного начисления награды — не только на уровне мока в
 * юнит-тесте, а на настоящем PostgreSQL под конкурентной нагрузкой.
 */
@DisplayName("Идемпотентность начисления награды (PUT /children/{id}/reward)")
class ChildRewardIdempotencyIT extends AbstractIntegrationTest {

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

    @Autowired
    private RewardGrantRepository rewardGrantRepository;

    private UUID childId;

    @BeforeEach
    void setUp() {
        rewardGrantRepository.deleteAll();
        childRepository.deleteAll();
        parentRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .email("parent-reward@example.com")
                .password("hashed_pass")
                .role(Role.PARENT)
                .active(true)
                .build();
        user = userRepository.save(user);

        Parent parent = Parent.builder()
                .user(user)
                .firstName("Иван")
                .surname("Петров")
                .build();
        parent = parentRepository.save(parent);

        Child child = Child.builder()
                .parent(parent)
                .firstName("Алиса")
                .surname("Петрова")
                .loginToken("TOKEN-REWARD")
                .exp(0)
                .coins(0)
                .level(1)
                .difficultyTrajectory(DifficultyTrajectory.NORMAL)
                .build();
        childId = childRepository.save(child).getId();
    }

    private RequestPostProcessor parentAuth() {
        return authentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARENT"))
        ));
    }

    @Test
    @DisplayName("Повторный HTTP-запрос с тем же ID назначения не должен начислить награду дважды")
    void shouldNotDoubleGrantOnRepeatedRequest() throws Exception {
        // Given
        UUID assignmentId = UUID.randomUUID();
        RewardRequest request = new RewardRequest(25, 10, false, assignmentId);

        // When — тот же запрос отправляется дважды (например, ретрай после сетевого сбоя)
        mockMvc.perform(put("/children/{childId}/reward", childId)
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/children/{childId}/reward", childId)
                        .with(parentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then — баланс отражает начисление ровно один раз
        Child child = childRepository.findById(childId).orElseThrow();
        assertThat(child.getExp()).isEqualTo(25);
        assertThat(child.getCoins()).isEqualTo(10);
    }

    @Test
    @DisplayName("Два конкурентных запроса с тем же ID назначения — награда начисляется ровно один раз")
    void shouldNotDoubleGrantOnConcurrentRequests() throws Exception {
        // Given
        UUID assignmentId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new RewardRequest(25, 10, false, assignmentId));

        // Тестовый метод обёрнут в единую транзакцию (@Transactional на AbstractIntegrationTest),
        // но настоящая гонка требует двух РЕАЛЬНЫХ, независимых транзакций/соединений —
        // поэтому фиксируем состояние setUp() и выходим из тестовой транзакции.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        Callable<Integer> call = () -> {
            ready.countDown();
            go.await();
            return mockMvc.perform(put("/children/{childId}/reward", childId)
                            .with(parentAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();
        };

        try {
            Future<Integer> f1 = pool.submit(call);
            Future<Integer> f2 = pool.submit(call);
            ready.await();
            go.countDown();

            int status1 = f1.get(10, TimeUnit.SECONDS);
            int status2 = f2.get(10, TimeUnit.SECONDS);

            // Both requests are individually idempotent, so both should succeed —
            // only the balance mutation must not double-apply.
            assertThat(List.of(status1, status2)).containsExactly(200, 200);
        } finally {
            pool.shutdown();
        }

        // Then — баланс отражает начисление ровно один раз, несмотря на гонку
        Child child = childRepository.findById(childId).orElseThrow();
        assertThat(child.getExp()).isEqualTo(25);
        assertThat(child.getCoins()).isEqualTo(10);
        assertThat(rewardGrantRepository.count()).isEqualTo(1);

        // Восстанавливаем транзакцию, чтобы штатный rollback-после-теста отработал как обычно.
        TestTransaction.start();
    }
}
