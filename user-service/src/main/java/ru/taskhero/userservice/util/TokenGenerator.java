package ru.taskhero.userservice.util;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Утилитный класс для генерации уникальных токенов.
 */
@UtilityClass
public class TokenGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TOKEN_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Генерация уникального loginToken для ребёнка.
     * Формат: 16 символов (буквы A-Z и цифры 0-9).
     * Пример: "A7K9M2P5Q8R3T6W1"
     *
     * @return уникальный токен
     */
    public static String generateLoginToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);

        for (int i = 0; i < TOKEN_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            token.append(CHARACTERS.charAt(index));
        }

        return token.toString();
    }

    /**
     * Генерация простого UUID-based токена (альтернативный вариант).
     *
     * @return токен на основе UUID без дефисов
     */
    public static String generateUuidToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, TOKEN_LENGTH);
    }
}
