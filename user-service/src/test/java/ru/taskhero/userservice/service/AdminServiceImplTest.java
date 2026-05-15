package ru.taskhero.userservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.SystemStatisticsDto;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.ParentRepository;
import ru.taskhero.userservice.repository.UserRepository;
import ru.taskhero.userservice.service.impl.AdminServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private ChildRepository childRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    @DisplayName("Должен вернуть статистику системы")
    void shouldGetSystemStatistics() {
        // Given
        when(userRepository.count()).thenReturn(10L);
        when(parentRepository.count()).thenReturn(6L);
        when(childRepository.count()).thenReturn(4L);
        when(userRepository.countByActive(true)).thenReturn(8L);
        when(userRepository.countByActive(false)).thenReturn(2L);
        when(userRepository.countByRole(Role.PARENT)).thenReturn(6L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(Role.CHILD)).thenReturn(3L);

        // When
        SystemStatisticsDto result = adminService.getSystemStatistics();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalUsers()).isEqualTo(10L);
        assertThat(result.totalParents()).isEqualTo(6L);
        assertThat(result.totalChildren()).isEqualTo(4L);
        assertThat(result.activeUsers()).isEqualTo(8L);
        assertThat(result.inactiveUsers()).isEqualTo(2L);
        assertThat(result.usersByRole()).containsKey(Role.PARENT);
        assertThat(result.usersByRole().get(Role.PARENT)).isEqualTo(6L);
    }
}
