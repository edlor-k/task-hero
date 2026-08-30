package ru.taskhero.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.taskhero.userservice.entity.RewardGrant;
import ru.taskhero.userservice.repository.RewardGrantRepository;

import java.util.UUID;

/**
 * Изолирует попытку зарегистрировать начисление награды в отдельной транзакции
 * (REQUIRES_NEW), чтобы нарушение уникального ограничения на {@code source_assignment_id}
 * не помечало rollback-only транзакцию вызывающего метода.
 * <p>
 * В PostgreSQL любая ошибка SQL (включая нарушение unique-ограничения) переводит
 * текущую транзакцию в состояние aborted — последующие запросы в НЕЙ ЖЕ транзакции
 * будут отклонены до отката. Если бы попытка вставки и последующее чтение баланса
 * ребёнка выполнялись в одной транзакции, перехват исключения в Java не спас бы —
 * СУБД всё равно отклонила бы следующий SELECT. Отдельная транзакция здесь —
 * не оптимизация, а необходимое условие корректности.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RewardGrantGuard {

    private final RewardGrantRepository rewardGrantRepository;

    /**
     * Попытаться атомарно зарезервировать начисление за {@code sourceAssignmentId}.
     *
     * @return {@code true}, если это первая регистрация (баланс нужно начислить),
     *         {@code false}, если награда за это назначение уже была начислена ранее
     *         (баланс начислять не нужно — идемпотентный повтор)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryRecord(UUID childId, UUID sourceAssignmentId, int exp, int coins) {
        try {
            rewardGrantRepository.saveAndFlush(RewardGrant.builder()
                    .childId(childId)
                    .sourceAssignmentId(sourceAssignmentId)
                    .exp(exp)
                    .coins(coins)
                    .build());
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Награда за назначение {} уже зарегистрирована — повторное начисление пропущено",
                    sourceAssignmentId);
            return false;
        }
    }
}
