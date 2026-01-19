package ru.taskhero.userservice.service;

import ru.taskhero.userservice.dto.SystemStatisticsDto;

/**
 * Сервис для административных операций и статистики.
 */
public interface AdminService {

    /**
     * Получить общую статистику системы.
     */
    SystemStatisticsDto getSystemStatistics();
}
