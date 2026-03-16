package ru.taskhero.taskservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Главный класс приложения Task Service.
 * Отвечает за управление заданиями, шаблонами и наградами.
 */
@SpringBootApplication(scanBasePackages = {
        "ru.taskhero.taskservice",
        "ru.taskhero.common"
})
@EnableFeignClients
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
