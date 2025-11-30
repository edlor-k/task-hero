package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.common.aop.LogMethod;
import ru.taskhero.common.model.enums.Role;
import ru.taskhero.userservice.dto.SystemStatisticsDto;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.repository.ParentRepository;
import ru.taskhero.userservice.repository.UserRepository;
import ru.taskhero.userservice.service.AdminService;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Реализация AdminService для административных операций.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;

    /**
     * Получить общую статистику системы.
     *
     * @return DTO со статистикой
     */
    @Override
    @Transactional(readOnly = true)
    @LogMethod("admin-get-statistics")
    public SystemStatisticsDto getSystemStatistics() {
        log.info("Сбор статистики системы");

        long totalUsers = userRepository.count();
        long totalParents = parentRepository.count();
        long totalChildren = childRepository.count();
        long activeUsers = userRepository.countByActive(true);
        long inactiveUsers = userRepository.countByActive(false);

        // Подсчет пользователей по ролям
        Map<Role, Long> usersByRole = Arrays.stream(Role.values())
                .collect(Collectors.toMap(
                        role -> role,
                        role -> userRepository.countByRole(role)
                ));

        log.info("Статистика: users={}, parents={}, children={}, active={}, inactive={}",
                totalUsers, totalParents, totalChildren, activeUsers, inactiveUsers);

        return new SystemStatisticsDto(
                totalUsers,
                totalParents,
                totalChildren,
                activeUsers,
                inactiveUsers,
                usersByRole
        );
    }
}
