package ru.taskhero.app.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Разбор значений HTML {@code <input type="datetime-local">} в {@link Instant}.
 * <p>
 * Регрессия «Ошибка назначения задания»: сервер и клиентский JS (см.
 * {@code static/js/app.js}, глобальный обработчик {@code submit} на ВСЕХ формах)
 * независимо друг от друга решали одну и ту же задачу конвертации значения
 * {@code datetime-local} и в итоге стали противоречить друг другу.
 * <ul>
 *     <li>Браузер отдаёт значение поля без секунд и часового пояса, например
 *     {@code "2026-08-27T18:00"}.</li>
 *     <li>{@code app.js} перед отправкой формы конвертирует это значение через
 *     {@code new Date(value).toISOString()} в полный ISO-Z instant, например
 *     {@code "2026-08-27T15:00:00.000Z"} — используя часовой пояс БРАУЗЕРА, что
 *     точнее, чем любая зона, известная серверу.</li>
 * </ul>
 * Этот метод принимает оба формата: сначала пробует разобрать значение как
 * готовый {@link Instant}/{@link OffsetDateTime} (то, что реально приходит от
 * {@code app.js} на практике), и только если это не удалось — как сырое
 * локальное значение без зоны (на случай, если JS не выполнился, например
 * отключён в браузере). Часовой пояс семьи нигде в модели не хранится, поэтому
 * для сырого локального значения используется зона сервера
 * ({@link ZoneId#systemDefault()}) как fallback.
 */
public final class FormDateTimeParser {

    private FormDateTimeParser() {
    }

    /**
     * Преобразовать значение {@code datetime-local} (сырое или уже сконвертированное
     * клиентским JS в ISO-Z/со смещением) в {@link Instant}.
     *
     * @param value значение поля формы (может быть {@code null}/пустым)
     * @return {@link Instant} или {@code null}, если значение не задано
     * @throws IllegalArgumentException если значение задано, но не является корректной датой/временем
     */
    public static Instant parseToInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();

        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // Не полный instant с "Z" — пробуем другие форматы ниже.
        }
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException ignored) {
            // Нет смещения — это сырое локальное значение без часового пояса.
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректный формат даты/времени: " + value, e);
        }
    }
}
