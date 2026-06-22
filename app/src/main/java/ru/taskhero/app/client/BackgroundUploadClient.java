package ru.taskhero.app.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import ru.taskhero.app.dto.BackgroundOptionDto;

/**
 * Отдельный Feign-клиент для multipart-загрузки фона.
 * Использует тот же multipart-кодер, что и {@link AvatarUploadClient}.
 */
@FeignClient(
        name = "user-service-background-upload",
        url = "${services.user-service.url}",
        configuration = AvatarUploadFeignConfig.class
)
public interface BackgroundUploadClient {

    @PostMapping(value = "/admin/background-options", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    BackgroundOptionDto upload(@RequestPart("file") MultipartFile file,
                               @RequestParam(value = "label", required = false) String label);
}
