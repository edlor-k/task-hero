package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.taskhero.common.exception.ResourceNotFoundException;
import ru.taskhero.common.exception.ValidationException;
import ru.taskhero.userservice.dto.BackgroundOptionResponseDto;
import ru.taskhero.userservice.entity.BackgroundOption;
import ru.taskhero.userservice.mapper.BackgroundOptionMapper;
import ru.taskhero.userservice.repository.BackgroundOptionRepository;
import ru.taskhero.userservice.repository.ChildRepository;
import ru.taskhero.userservice.service.BackgroundOptionService;
import ru.taskhero.userservice.service.ObjectStorageService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackgroundOptionServiceImpl implements BackgroundOptionService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/webp", "image/jpeg");
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/png", "png",
            "image/webp", "webp",
            "image/jpeg", "jpg"
    );

    private final BackgroundOptionRepository backgroundOptionRepository;
    private final ChildRepository childRepository;
    private final BackgroundOptionMapper backgroundOptionMapper;
    private final ObjectStorageService objectStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<BackgroundOptionResponseDto> listActive() {
        return backgroundOptionRepository.findAllActiveOrdered().stream()
                .map(backgroundOptionMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BackgroundOptionResponseDto> listAll() {
        return backgroundOptionRepository.findAllOrdered().stream()
                .map(backgroundOptionMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BackgroundOptionResponseDto create(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Файл картинки не передан");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException("Допустимые форматы: PNG, WebP, JPEG");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать загруженный файл", e);
        }

        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        String key = "backgrounds/" + UUID.randomUUID() + "." + extension;
        String imageUrl = objectStorageService.upload(key, bytes, contentType);

        int nextSortOrder = (int) backgroundOptionRepository.count();
        BackgroundOption backgroundOption = BackgroundOption.builder()
                .imageUrl(imageUrl)
                .imageKey(key)
                .label(label != null && !label.isBlank() ? label.trim() : null)
                .sortOrder(nextSortOrder)
                .active(true)
                .build();

        backgroundOption = backgroundOptionRepository.save(backgroundOption);
        log.info("Загружена новая опция фона: {}", backgroundOption.getId());
        return backgroundOptionMapper.toDto(backgroundOption);
    }

    @Override
    @Transactional
    public BackgroundOptionResponseDto setActive(UUID id, boolean active) {
        BackgroundOption backgroundOption = backgroundOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Опция фона с ID " + id + " не найдена"));
        backgroundOption.setActive(active);
        backgroundOption = backgroundOptionRepository.save(backgroundOption);
        log.info("Опция фона {} переключена: active={}", id, active);
        return backgroundOptionMapper.toDto(backgroundOption);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        BackgroundOption backgroundOption = backgroundOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Опция фона с ID " + id + " не найдена"));

        if (childRepository.existsByBackgroundOptionId(id)) {
            throw new ValidationException(
                    "Этот фон выбран хотя бы одним ребёнком — деактивируйте его, а не удаляйте");
        }

        objectStorageService.delete(backgroundOption.getImageKey());
        backgroundOptionRepository.delete(backgroundOption);
        log.info("Опция фона {} удалена", id);
    }
}
