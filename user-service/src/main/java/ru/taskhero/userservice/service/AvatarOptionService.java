package ru.taskhero.userservice.service;

import org.springframework.web.multipart.MultipartFile;
import ru.taskhero.userservice.dto.AvatarOptionResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Сервис управления галереей аватаров.
 */
public interface AvatarOptionService {

    /**
     * Активные опции — для пикера ребёнка.
     */
    List<AvatarOptionResponseDto> listActive();

    /**
     * Все опции (вкл. неактивные) — для админки.
     */
    List<AvatarOptionResponseDto> listAll();

    /**
     * Загрузить новую картинку в галерею (админ).
     */
    AvatarOptionResponseDto create(MultipartFile file, String label);

    /**
     * Включить/выключить видимость опции в пикере ребёнка.
     */
    AvatarOptionResponseDto setActive(UUID id, boolean active);

    /**
     * Обновить отображаемое название аватара.
     */
    AvatarOptionResponseDto updateLabel(UUID id, String label);

    /**
     * Удалить опцию из галереи (и файл из хранилища).
     */
    void delete(UUID id);
}
