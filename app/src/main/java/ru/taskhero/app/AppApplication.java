package ru.taskhero.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.util.TimeZone;

/**
 * Главный класс приложения TaskHero.
 * Точка входа с Thymeleaf UI, объединяющая все модули.
 * Не сканирует common, т.к. app не использует JPA.
 */
@SpringBootApplication(scanBasePackages = {
        "ru.taskhero.app"
})
@EnableFeignClients
public class AppApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        SpringApplication.run(AppApplication.class, args);
    }
}
