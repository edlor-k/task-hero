package ru.taskhero.userservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.taskhero.userservice.dto.AuditLogDto;
import ru.taskhero.userservice.entity.AuditAction;
import ru.taskhero.userservice.entity.AuditLog;
import ru.taskhero.userservice.repository.AuditLogRepository;
import ru.taskhero.userservice.service.impl.AuditServiceImpl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Unit Tests")
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    @Test
    @DisplayName("Должен записать событие аудита с email")
    void shouldLogAuditEventWithEmail() {
        // Given
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AuditLog savedLog = AuditLog.builder()
                .adminId(adminId)
                .adminEmail("admin@example.com")
                .action(AuditAction.USER_REGISTERED)
                .targetType("USER")
                .targetId(targetId)
                .details("Регистрация")
                .build();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);

        // When
        auditService.log(adminId, "admin@example.com", AuditAction.USER_REGISTERED, "USER", targetId, "Регистрация");

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog captured = captor.getValue();
        assertThat(captured.getAdminEmail()).isEqualTo("admin@example.com");
        assertThat(captured.getAction()).isEqualTo(AuditAction.USER_REGISTERED);
    }

    @Test
    @DisplayName("Должен использовать сгенерированный email если email не указан")
    void shouldGenerateEmailWhenBlank() {
        // Given
        UUID adminId = UUID.randomUUID();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        auditService.log(adminId, "", AuditAction.USER_BLOCKED, "USER", UUID.randomUUID(), "test");

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAdminEmail()).contains(adminId.toString());
    }

    @Test
    @DisplayName("Должен использовать системный email если adminId и email не указаны")
    void shouldUseSystemEmailWhenNoAdminInfo() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        auditService.log(null, null, AuditAction.USER_REGISTERED, "USER", UUID.randomUUID(), "test");

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAdminEmail()).isEqualTo("system@taskhero.local");
    }

    @Test
    @DisplayName("Должен вернуть все записи аудита с пагинацией")
    void shouldGetAllAuditLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = buildAuditLog();
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        // When
        Page<AuditLogDto> result = auditService.getAuditLogs(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).action()).isEqualTo(AuditAction.USER_REGISTERED);
    }

    @Test
    @DisplayName("Должен вернуть записи аудита с фильтрами")
    void shouldGetFilteredAuditLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        AuditLog log = buildAuditLog();
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findFiltered(eq("USER_REGISTERED"), eq(from), eq(to), eq(pageable)))
                .thenReturn(page);

        // When
        Page<AuditLogDto> result = auditService.getAuditLogs(pageable, AuditAction.USER_REGISTERED, from, to);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findFiltered("USER_REGISTERED", from, to, pageable);
    }

    @Test
    @DisplayName("Должен вернуть записи аудита с null фильтрами")
    void shouldGetAuditLogsWithNullFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = buildAuditLog();
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findFiltered(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        // When
        Page<AuditLogDto> result = auditService.getAuditLogs(pageable, null, null, null);

        // Then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Должен вернуть последние N записей аудита")
    void shouldGetRecentLogs() {
        // Given
        AuditLog log = buildAuditLog();
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        // When
        Page<AuditLogDto> result = auditService.getRecentLogs(5);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    private AuditLog buildAuditLog() {
        return AuditLog.builder()
                .id(UUID.randomUUID())
                .adminId(UUID.randomUUID())
                .adminEmail("admin@example.com")
                .action(AuditAction.USER_REGISTERED)
                .targetType("USER")
                .targetId(UUID.randomUUID())
                .details("Тест")
                .createdAt(Instant.now())
                .build();
    }
}
