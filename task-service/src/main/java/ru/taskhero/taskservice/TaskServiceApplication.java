package ru.taskhero.taskservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * Главный класс приложения Task Service.
 * Отвечает за управление заданиями, шаблонами и наградами.
 */
@SpringBootApplication(scanBasePackages = {
        "ru.taskhero.taskservice",
        "ru.taskhero.common"
})
@EnableFeignClients
@EnableScheduling
public class TaskServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
