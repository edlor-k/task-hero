package ru.taskhero.app.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Регрессионный тест на баг «Ошибка назначения задания». Корневая причина —
 * конфликт двух независимых реализаций конвертации {@code <input type="datetime-local">}:
 * {@code app.js} на клиенте глобально конвертирует значение ВСЕХ таких полей в полный
 * ISO-Z instant (например, {@code "2026-08-27T15:00:00.000Z"}) перед отправкой формы,
 * а сервер ожидал сырое локальное значение без зоны (например, {@code "2026-08-27T18:00"}).
 * Из-за этого КАЖДОЕ назначение с дедлайном отклонялось как «Некорректный формат дедлайна».
 * Парсер должен одинаково надёжно принимать оба формата.
 */
@DisplayName("FormDateTimeParser")
class FormDateTimeParserTest {

    @Test
    @DisplayName("Должен разобрать ISO-Z значение — именно то, что реально отправляет app.js (валидный дедлайн)")
    void shouldParseIsoInstantSentByAppJs() {
        // Given — именно так app.js отправляет дедлайн на сервер после new Date(value).toISOString()
        String isoZValue = "2026-08-27T15:00:00.000Z";

        // When
        Instant result = FormDateTimeParser.parseToInstant(isoZValue);

        // Then
        assertThat(result).isEqualTo(Instant.parse(isoZValue));
    }

    @Test
    @DisplayName("Должен разобрать значение со смещением часового пояса (валидный дедлайн)")
    void shouldParseOffsetDateTime() {
        String value = "2026-08-27T18:00:00+03:00";

        Instant result = FormDateTimeParser.parseToInstant(value);

        assertThat(result).isEqualTo(OffsetDateTime.of(2026, 8, 27, 18, 0, 0, 0, ZoneOffset.ofHours(3)).toInstant());
    }

    @Test
    @DisplayName("Не должен падать на сыром значении datetime-local без зоны (fallback без JS)")
    void shouldParseRawDatetimeLocalValueWithoutSecondsOrOffset() {
        // Given — так браузер отдаёт значение поля datetime-local, если конвертация JS не произошла
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
    @DisplayName("Должен корректно разбирать сырое значение с секундами")
    void shouldParseValueWithSeconds() {
        Instant result = FormDateTimeParser.parseToInstant("2026-09-01T09:30:15");
        Instant expected = LocalDateTime.of(2026, 9, 1, 9, 30, 15)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен выбросить понятную ошибку на некорректном значении (невалидный дедлайн)")
    void shouldThrowIllegalArgumentExceptionOnGarbage() {
        assertThatThrownBy(() -> FormDateTimeParser.parseToInstant("не дата"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Должен выбросить понятную ошибку на структурно похожей, но некорректной дате (невалидный дедлайн)")
    void shouldThrowIllegalArgumentExceptionOnInvalidCalendarDate() {
        // 32 февраля — синтаксически похоже на дату, но не существует
        assertThatThrownBy(() -> FormDateTimeParser.parseToInstant("2026-02-32T10:00"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
