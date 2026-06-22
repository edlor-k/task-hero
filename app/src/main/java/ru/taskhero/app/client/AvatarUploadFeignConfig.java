package ru.taskhero.app.client;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.context.annotation.Bean;

/**
 * Конфигурация только для {@link AvatarUploadClient} (multipart-кодер).
 *
 * <p>ВАЖНО: этот класс намеренно НЕ аннотирован {@code @Configuration}.
 * Spring Cloud OpenFeign регистрирует {@code @Bean}-методы из классов,
 * переданных в {@code @FeignClient(configuration = ...)}, в изолированном
 * дочернем контексте конкретного клиента — независимо от {@code @Configuration}.
 * Если аннотировать класс {@code @Configuration}, обычный component scan
 * приложения подхватит его и сделает {@link SpringFormEncoder} ГЛОБАЛЬНЫМ
 * энкодером для всех Feign-клиентов, что сломает обычные JSON-запросы
 * (UserServiceClient, TaskServiceClient).</p>
 */
public class AvatarUploadFeignConfig {

    @Bean
    public Encoder feignFormEncoder() {
        return new SpringFormEncoder();
    }
}
