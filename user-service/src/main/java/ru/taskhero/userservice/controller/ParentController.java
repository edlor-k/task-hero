package ru.taskhero.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.taskhero.common.exception.ExceptionBody;
import ru.taskhero.userservice.dto.ParentResponseDto;
import ru.taskhero.userservice.dto.UpdateParentRequest;
import ru.taskhero.userservice.service.ParentService;
import ru.taskhero.userservice.util.SecurityUtils;

import java.util.UUID;

/**
 * Контроллер для работы с профилем родителя.
 * Все endpoints требуют аутентификации (JWT токен) и роли PARENT.
 */
@Slf4j
@RestController
@RequestMapping("/parents")
@RequiredArgsConstructor
@Tag(name = "Parents", description = "Управление профилями родителей")
@SecurityRequirement(name = "Bearer Authentication")
public class ParentController {

    private final ParentService parentService;

    /**
     * Получить профиль текущего родителя.
     *
     * @return DTO профиля родителя
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Получить профиль текущего родителя",
            description = "Возвращает профиль родителя для текущего аутентифицированного пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Профиль родителя получен",
                    content = @Content(schema = @Schema(implementation = ParentResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав (требуется роль PARENT)",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Профиль родителя не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<ParentResponseDto> getCurrentParent() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Запрос профиля родителя для пользователя: {}", currentUserId);

        ParentResponseDto response = parentService.getByUserId(currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновить профиль текущего родителя.
     *
     * @param request данные для обновления
     * @return обновленный профиль
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(
            summary = "Обновить профиль текущего родителя",
            description = "Позволяет родителю обновить свое имя и фамилию"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Профиль обновлен",
                    content = @Content(schema = @Schema(implementation = ParentResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Профиль не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionBody.class))
            )
    })
    public ResponseEntity<ParentResponseDto> updateCurrentParent(@Valid @RequestBody UpdateParentRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Обновление профиля родителя для пользователя: {}", currentUserId);

        ParentResponseDto response = parentService.updateCurrentParent(currentUserId, request);
        return ResponseEntity.ok(response);
    }
}
