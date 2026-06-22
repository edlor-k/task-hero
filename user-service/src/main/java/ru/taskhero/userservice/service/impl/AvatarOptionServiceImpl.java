package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.userservice.dto.AvatarOptionResponseDto;
import ru.taskhero.userservice.entity.AvatarOption;
import ru.taskhero.userservice.mapper.AvatarOptionMapper;
import ru.taskhero.userservice.repository.AvatarOptionRepository;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.service.AvatarOptionService;
import ru.taskhero.userservice.service.ObjectStorageService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Имплементация управления галереей аватаров.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarOptionServiceImpl implements AvatarOptionService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/webp");
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final int MIN_DIMENSION = 256;
    private static final int MAX_DIMENSION = 1024;

    private final AvatarOptionRepository avatarOptionRepository;
    private final ChildRepository childRepository;
    private final AvatarOptionMapper avatarOptionMapper;
    private final ObjectStorageService objectStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<AvatarOptionResponseDto> listActive() {
        return avatarOptionRepository.findAllActiveOrdered().stream()
                .map(avatarOptionMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvatarOptionResponseDto> listAll() {
        return avatarOptionRepository.findAllOrdered().stream()
                .map(avatarOptionMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AvatarOptionResponseDto create(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Файл картинки не передан");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException("Допустимые форматы: PNG, WebP");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать загруженный файл", e);
        }

        validateSquareDimensions(bytes);

        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        String key = "avatars/" + UUID.randomUUID() + "." + extension;
        String imageUrl = objectStorageService.upload(key, bytes, contentType);

        int nextSortOrder = (int) avatarOptionRepository.count();
        AvatarOption avatarOption = AvatarOption.builder()
                .imageUrl(imageUrl)
                .imageKey(key)
                .label(label != null && !label.isBlank() ? label.trim() : null)
                .sortOrder(nextSortOrder)
                .active(true)
                .build();

        avatarOption = avatarOptionRepository.save(avatarOption);
        log.info("Загружена новая опция аватара: {}", avatarOption.getId());
        return avatarOptionMapper.toDto(avatarOption);
    }

    @Override
    @Transactional
    public AvatarOptionResponseDto setActive(UUID id, boolean active) {
        AvatarOption avatarOption = avatarOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Опция аватара с ID " + id + " не найдена"));
        avatarOption.setActive(active);
        avatarOption = avatarOptionRepository.save(avatarOption);
        log.info("Опция аватара {} переключена: active={}", id, active);
        return avatarOptionMapper.toDto(avatarOption);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AvatarOption avatarOption = avatarOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Опция аватара с ID " + id + " не найдена"));

        if (childRepository.existsByAvatarOptionId(id)) {
            throw new ValidationException(
                    "Этот аватар выбран хотя бы одним ребёнком — деактивируйте его, а не удаляйте");
        }

        objectStorageService.delete(avatarOption.getImageKey());
        avatarOptionRepository.delete(avatarOption);
        log.info("Опция аватара {} удалена", id);
    }

    /**
     * Проверяет, что картинка квадратная и её сторона в допустимом диапазоне.
     */
    private void validateSquareDimensions(byte[] bytes) {
        BufferedImage image;
        try {
            image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать изображение", e);
        }
        if (image == null) {
            throw new ValidationException("Файл не распознан как изображение");
        }
        if (image.getWidth() != image.getHeight()) {
            throw new ValidationException("Картинка должна быть квадратной (ширина == высоте)");
        }
        if (image.getWidth() < MIN_DIMENSION || image.getWidth() > MAX_DIMENSION) {
            throw new ValidationException(
                    "Размер картинки должен быть от " + MIN_DIMENSION + " до " + MAX_DIMENSION + " px");
        }
    }
}
