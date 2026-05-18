package ru.taskhero.taskservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.taskhero.taskservice.dto.ChildResponseDto;
import ru.taskhero.taskservice.dto.RewardRequest;

import java.util.UUID;

/**
 * Feign клиент для взаимодействия с User Service.
 * Используется для получения информации о детях и начисления наград.
 */
@FeignClient(
        name = "user-service",
        url = "${user-service.url}"
)
public interface UserServiceClient {

    /**
     * Получить информацию о ребёнке по ID.
     *
     * @param childId ID ребёнка
     * @return информация о ребёнке
     */
    @GetMapping("/children/{childId}")
    ChildResponseDto getChildById(@PathVariable("childId") UUID childId);

    /**
     * Начислить награды ребёнку.
     *
     * @param childId ID ребёнка
     * @param request данные о награде (exp, coins)
     * @return обновленная информация о ребёнке
     */
    @PutMapping("/children/{childId}/reward")
    ChildResponseDto addReward(
            @PathVariable("childId") UUID childId,
            @RequestBody RewardRequest request
    );

    /**
     * Проверить, принадлежит ли ребёнок родителю.
     *
     * @param childId  ID ребёнка
     * @param parentId ID родителя
     * @return true если ребёнок принадлежит родителю
     */
    @GetMapping("/children/{childId}/belongs-to/{parentId}")
    Boolean isChildBelongsToParent(
            @PathVariable("childId") UUID childId,
            @PathVariable("parentId") UUID parentId
    );

    /**
     * Разблокировать изменение никнейма ребёнка.
     * Вызывается при автоматическом одобрении вводного задания.
     */
    @PostMapping("/children/{childId}/unlock-nickname")
    void unlockNickname(@PathVariable("childId") UUID childId);
}
