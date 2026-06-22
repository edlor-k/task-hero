package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.userservice.config.ObjectStorageProperties;
import ru.taskhero.userservice.service.ObjectStorageService;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Реализация {@link ObjectStorageService} через AWS SDK v2 S3-клиент.
 * Работает с любым S3-совместимым провайдером (Яндекс Object Storage,
 * Selectel, Cloudflare R2, AWS S3, MinIO) — смена провайдера это просто
 * смена endpoint/credentials в конфиге, без изменения кода.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ObjectStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final ObjectStorageProperties properties;

    @Override
    public String upload(String key, byte[] content, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    // Бакет/префикс avatars/ публичный на чтение — картинки отдаются
                    // напрямую из хранилища, без проксирования через наш app-контейнер.
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    // Ключ содержит UUID — файл неизменяемый, можно кэшировать надолго.
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));
            log.info("Файл {} загружен в бакет {}", key, properties.getBucket());
            return buildPublicUrl(key);
        } catch (S3Exception e) {
            log.error("Ошибка загрузки файла {} в объектное хранилище: {}", key, e.getMessage());
            throw new ValidationException("Не удалось загрузить файл в хранилище");
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
            log.info("Файл {} удалён из бакета {}", key, properties.getBucket());
        } catch (S3Exception e) {
            log.error("Ошибка удаления файла {} из объектного хранилища: {}", key, e.getMessage());
        }
    }

    private String buildPublicUrl(String key) {
        String base = properties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = properties.getEndpoint() + "/" + properties.getBucket();
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + key;
    }
}
