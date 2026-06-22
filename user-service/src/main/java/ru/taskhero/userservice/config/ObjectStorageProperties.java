package ru.taskhero.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация S3-совместимого объектного хранилища (галерея аватаров).
 * Совместимо с Яндекс Object Storage, Selectel S3, Cloudflare R2, AWS S3,
 * а при необходимости — и с самостоятельным MinIO (просто смена endpoint).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "object-storage")
public class ObjectStorageProperties {

    /** Endpoint S3 API провайдера, например https://storage.yandexcloud.net */
    private String endpoint;

    /** Регион (для большинства S3-совместимых провайдеров — произвольная строка). */
    private String region = "ru-central1";

    /** Имя бакета. */
    private String bucket;

    private String accessKey;

    private String secretKey;

    /**
     * Базовый публичный URL для отдачи файлов клиенту, например
     * {@code https://storage.yandexcloud.net/my-bucket}. Если не задан —
     * собирается из endpoint + bucket.
     */
    private String publicBaseUrl;
}
