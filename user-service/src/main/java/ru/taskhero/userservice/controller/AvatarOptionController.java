package ru.taskhero.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.taskhero.userservice.dto.AvatarOptionResponseDto;
import ru.taskhero.userservice.service.AvatarOptionService;

import java.util.List;

/**
 * Публичный (для роли CHILD) список доступных аватаров — пикер при выборе
 * аватара. Управление галереей (загрузка/деактивация/удаление) — в
 * {@link AdminController}.
 */
@Slf4j
@RestController
@RequestMapping("/avatar-options")
@RequiredArgsConstructor
@Tag(name = "Avatar options", description = "Галерея аватаров для выбора ребёнком")
@SecurityRequirement(name = "Bearer Authentication")
public class AvatarOptionController {

    private final AvatarOptionService avatarOptionService;

    @GetMapping
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Активные опции аватара", description = "Список аватаров, доступных ребёнку для выбора")
    public ResponseEntity<List<AvatarOptionResponseDto>> listActive() {
        return ResponseEntity.ok(avatarOptionService.listActive());
    }
}
