package ru.taskhero.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.userservice.entity.User;
import ru.taskhero.userservice.repository.UserRepository;

/**
 * Инициализатор мастер-администратора.
 * При старте приложения устанавливает/обновляет пароль
 * из переменной окружения ADMIN_PASSWORD.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.password:#{null}}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD не задан — пароль мастер-администратора не обновлён.");
            return;
        }

        userRepository.findByEmail("admin").ifPresent(admin -> {
            String encoded = passwordEncoder.encode(adminPassword);
            admin.setPassword(encoded);
            userRepository.save(admin);
            log.info("Пароль мастер-администратора (admin) обновлён из переменной окружения.");
        });
    }
}
