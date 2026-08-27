package ru.taskhero.app.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Регрессионный тест на баг «Ошибка назначения задания»: браузер отдаёт значение
 * {@code <input type="datetime-local">} без секунд и часового пояса
 * (например, {@code "2026-08-27T18:00"}), а {@link Instant#parse(CharSequence)}
 * такую строку отклоняет. До исправления это сырое значение уходило в JSON-запрос
 * к task-service как есть и приводило к 400 от Feign на каждом назначении с дедлайном.
 */
@DisplayName("FormDateTimeParser")
class FormDateTimeParserTest {

    @Test
    @DisplayName("Не должен падать на сыром значении datetime-local (баг «Ошибка назначения задания»)")
    void shouldParseRawDatetimeLocalValueWithoutSecondsOrOffset() {
        // Given — именно так браузер отдаёт значение поля datetime-local
        String rawBrowserValue = "2026-08-27T18:00";

        // Instant.parse на таком значении бросает DateTimeParseException — это и была причина бага
        assertThatThrownBy(() -> Instant.parse(rawBrowserValue))
                .isInstanceOf(java.time.format.DateTimeParseException.class);

        // When
        Instant result = FormDateTimeParser.parseToInstant(rawBrowserValue);

        // Then
        Instant expected = LocalDateTime.of(2026, 8, 27, 18, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен вернуть null для пустого значения (дедлайн не задан)")
    void shouldReturnNullForBlankValue() {
        assertThat(FormDateTimeParser.parseToInstant(null)).isNull();
        assertThat(FormDateTimeParser.parseToInstant("")).isNull();
        assertThat(FormDateTimeParser.parseToInstant("   ")).isNull();
    }

    @Test
    @DisplayName("Должен корректно разбирать значение с секундами")
    void shouldParseValueWithSeconds() {
        Instant result = FormDateTimeParser.parseToInstant("2026-09-01T09:30:15");
        Instant expected = LocalDateTime.of(2026, 9, 1, 9, 30, 15)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен выбросить понятную ошибку на некорректном значении")
    void shouldThrowIllegalArgumentExceptionOnGarbage() {
        assertThatThrownBy(() -> FormDateTimeParser.parseToInstant("не дата"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
