package ru.taskhero.userservice.service;

import org.springframework.web.multipart.MultipartFile;
import ru.taskhero.userservice.dto.BackgroundOptionResponseDto;

import java.util.List;
import java.util.UUID;

public interface BackgroundOptionService {

    List<BackgroundOptionResponseDto> listActive();

    List<BackgroundOptionResponseDto> listAll();

    BackgroundOptionResponseDto create(MultipartFile file, String label);

    BackgroundOptionResponseDto setActive(UUID id, boolean active);

    BackgroundOptionResponseDto updateLabel(UUID id, String label);

    void delete(UUID id);
}
