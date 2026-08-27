package ru.taskhero.app.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Разбор значений HTML {@code <input type="datetime-local">} в {@link Instant}.
 * <p>
 * Браузер присылает локальное время без секунд и часового пояса (например,
 * {@code "2026-08-27T18:00"}). {@link Instant#parse(CharSequence)} такую строку
 * не понимает (нужны секунды и смещение/"Z"), поэтому раньше сырое значение
 * дедлайна из формы уходило в task-service как есть, Jackson не мог его
 * десериализовать в {@code Instant}, Feign получал 400, а родитель видел
 * обезличенное «Ошибка назначения задания» без реальной причины.
 * <p>
 * Часовой пояс семьи нигде в модели не хранится, поэтому используется зона
 * сервера ({@link ZoneId#systemDefault()}) как единственная разумная точка
 * отсчёта на первой итерации.
 */
public final class FormDateTimeParser {

    private FormDateTimeParser() {
    }

    /**
     * Преобразовать значение {@code datetime-local} в {@link Instant}.
     *
     * @param value значение поля формы, например {@code "2026-08-27T18:00"} (может быть {@code null}/пустым)
     * @return {@link Instant} или {@code null}, если значение не задано
     * @throws IllegalArgumentException если значение задано, но не является корректной датой/временем
     */
    public static Instant parseToInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(value);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректный формат даты/времени: " + value, e);
        }
    }
}
