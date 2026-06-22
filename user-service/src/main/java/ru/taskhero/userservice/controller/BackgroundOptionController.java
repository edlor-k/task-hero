package ru.taskhero.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.taskhero.userservice.dto.BackgroundOptionResponseDto;
import ru.taskhero.userservice.service.BackgroundOptionService;

import java.util.List;

@RestController
@RequestMapping("/background-options")
@RequiredArgsConstructor
@Tag(name = "Background options", description = "Галерея фонов для выбора ребёнком")
@SecurityRequirement(name = "Bearer Authentication")
public class BackgroundOptionController {

    private final BackgroundOptionService backgroundOptionService;

    @GetMapping
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Активные фоны", description = "Список фонов, доступных ребёнку для выбора")
    public ResponseEntity<List<BackgroundOptionResponseDto>> listActive() {
        return ResponseEntity.ok(backgroundOptionService.listActive());
    }
}
