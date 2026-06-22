package ru.taskhero.app.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import ru.taskhero.app.dto.AvatarOptionDto;

/**
 * Отдельный Feign-клиент только для multipart-загрузки аватара.
 * Вынесен из {@link UserServiceClient}, чтобы скоуп-конфигурация с
 * multipart-кодером ({@link AvatarUploadFeignConfig}) не повлияла на
 * остальные (JSON) Feign-клиенты приложения.
 */
@FeignClient(
        name = "user-service-avatar-upload",
        url = "${services.user-service.url}",
        configuration = AvatarUploadFeignConfig.class
)
public interface AvatarUploadClient {

    @PostMapping(value = "/admin/avatar-options", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    AvatarOptionDto upload(@RequestPart("file") MultipartFile file,
                           @RequestParam(value = "label", required = false) String label);
}
