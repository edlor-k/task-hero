package ru.taskhero.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Точка запуска User Service
 */
@SpringBootApplication(scanBasePackages = "ru.taskhero")
@EnableJpaRepositories(basePackages = "ru.taskhero.userservice.repository")
@EntityScan(basePackages = {
        "ru.taskhero.common.model.entity",
        "ru.taskhero.userservice.entity"
})
@EnableFeignClients(basePackages = "ru.taskhero")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
