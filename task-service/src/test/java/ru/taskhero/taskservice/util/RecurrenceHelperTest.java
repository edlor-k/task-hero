package ru.taskhero.taskservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecurrenceHelper Unit Tests")
class RecurrenceHelperTest {

    @Test
    @DisplayName("Должен вернуть пустой список при null rrule")
    void shouldReturnEmptyForNullRrule() {
        List<LocalDate> result = RecurrenceHelper.getNextOccurrences(null, LocalDate.now(), 5);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Должен вернуть пустой список при пустом rrule")
    void shouldReturnEmptyForBlankRrule() {
        List<LocalDate> result = RecurrenceHelper.getNextOccurrences("  ", LocalDate.now(), 5);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Должен вернуть N ежедневных дат")
    void shouldReturnDailyDates() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        List<LocalDate> result = RecurrenceHelper.getNextOccurrences("FREQ=DAILY", start, 5);

        assertThat(result).hasSize(5);
        assertThat(result.get(0)).isEqualTo(start);
        assertThat(result.get(1)).isEqualTo(start.plusDays(1));
        assertThat(result.get(4)).isEqualTo(start.plusDays(4));
    }

    @Test
    @DisplayName("Должен вернуть только будние дни при FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR")
    void shouldReturnWeekdaysOnly() {
        LocalDate start = LocalDate.of(2025, 1, 6); // понедельник
        List<LocalDate> result = RecurrenceHelper.getNextOccurrences(
                "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", start, 5);

        assertThat(result).hasSize(5);
        result.forEach(date ->
                assertThat(date.getDayOfWeek()).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        );
    }

    @Test
    @DisplayName("Должен вернуть даты с интервалом FREQ=DAILY;INTERVAL=2")
    void shouldReturnEveryOtherDay() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        List<LocalDate> result = RecurrenceHelper.getNextOccurrences("FREQ=DAILY;INTERVAL=2", start, 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(start);
        assertThat(result.get(1)).isEqualTo(start.plusDays(2));
        assertThat(result.get(2)).isEqualTo(start.plusDays(4));
    }

    @Test
    @DisplayName("Должен вернуть еженедельные даты")
    void shouldReturnWeeklyDates() {
        LocalDate start = LocalDate.of(2025, 1, 6); // понедельник
        List<LocalDate> result = RecurrenceHelper.getNextOccurrences("FREQ=WEEKLY", start, 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(start);
        assertThat(result.get(1)).isEqualTo(start.plusWeeks(1));
        assertThat(result.get(2)).isEqualTo(start.plusWeeks(2));
    }

    @Test
    @DisplayName("Должен вернуть ежемесячные даты")
    void shouldReturnMonthlyDates() {
        LocalDate start = LocalDate.of(2025, 1, 15);
        List<LocalDate> result = RecurrenceHelper.getNextOccurrences("FREQ=MONTHLY", start, 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(start);
        assertThat(result.get(1)).isEqualTo(start.plusMonths(1));
        assertThat(result.get(2)).isEqualTo(start.plusMonths(2));
    }

    @Test
    @DisplayName("Должен вернуть даты 15-го числа каждого месяца")
    void shouldReturnMonthlyByDayOfMonth() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        List<LocalDate> result = RecurrenceHelper.getNextOccurrences(
                "FREQ=MONTHLY;BYMONTHDAY=15", start, 3);

        assertThat(result).hasSize(3);
        result.forEach(date -> assertThat(date.getDayOfMonth()).isEqualTo(15));
    }

    @Test
    @DisplayName("Должен вернуть пустой список при неизвестном FREQ")
    void shouldReturnEmptyForUnknownFreq() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        List<LocalDate> result = RecurrenceHelper.getNextOccurrences("FREQ=YEARLY", start, 3);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isValid должен вернуть true для FREQ=DAILY")
    void shouldBeValidForDaily() {
        assertThat(RecurrenceHelper.isValid("FREQ=DAILY")).isTrue();
    }

    @Test
    @DisplayName("isValid должен вернуть true для FREQ=WEEKLY")
    void shouldBeValidForWeekly() {
        assertThat(RecurrenceHelper.isValid("FREQ=WEEKLY;BYDAY=MO,WE")).isTrue();
    }

    @Test
    @DisplayName("isValid должен вернуть true для FREQ=MONTHLY")
    void shouldBeValidForMonthly() {
        assertThat(RecurrenceHelper.isValid("FREQ=MONTHLY;BYMONTHDAY=10")).isTrue();
    }

    @Test
    @DisplayName("isValid должен вернуть false для null")
    void shouldBeInvalidForNull() {
        assertThat(RecurrenceHelper.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("isValid должен вернуть false для неизвестной частоты")
    void shouldBeInvalidForUnknownFreq() {
        assertThat(RecurrenceHelper.isValid("FREQ=YEARLY")).isFalse();
    }

    @Test
    @DisplayName("toHumanReadable должен вернуть 'Без повторения' для null")
    void shouldReturnNoRepeatForNull() {
        assertThat(RecurrenceHelper.toHumanReadable(null)).isEqualTo("Без повторения");
    }

    @Test
    @DisplayName("toHumanReadable должен вернуть 'Ежедневно' для FREQ=DAILY")
    void shouldReturnDailyDescription() {
        assertThat(RecurrenceHelper.toHumanReadable("FREQ=DAILY")).isEqualTo("Ежедневно");
    }

    @Test
    @DisplayName("toHumanReadable должен вернуть 'Каждый будний день' для будней")
    void shouldReturnWeekdayDescription() {
        String rrule = "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR";
        assertThat(RecurrenceHelper.toHumanReadable(rrule)).isEqualTo("Каждый будний день");
    }

    @Test
    @DisplayName("toHumanReadable должен вернуть 'Еженедельно' для FREQ=WEEKLY")
    void shouldReturnWeeklyDescription() {
        assertThat(RecurrenceHelper.toHumanReadable("FREQ=WEEKLY")).isEqualTo("Еженедельно");
    }

    @Test
    @DisplayName("toHumanReadable должен вернуть 'Ежемесячно' для FREQ=MONTHLY")
    void shouldReturnMonthlyDescription() {
        assertThat(RecurrenceHelper.toHumanReadable("FREQ=MONTHLY")).isEqualTo("Ежемесячно");
    }

    @Test
    @DisplayName("toHumanReadable должен включать дату для FREQ=MONTHLY;BYMONTHDAY=15")
    void shouldReturnMonthlyWithDay() {
        assertThat(RecurrenceHelper.toHumanReadable("FREQ=MONTHLY;BYMONTHDAY=15"))
                .isEqualTo("Ежемесячно 15-го числа");
    }

    @Test
    @DisplayName("toHumanReadable для FREQ=DAILY;INTERVAL=3 должен вернуть 'Каждые 3 дн.'")
    void shouldReturnEveryNDays() {
        assertThat(RecurrenceHelper.toHumanReadable("FREQ=DAILY;INTERVAL=3"))
                .isEqualTo("Каждые 3 дн.");
    }
}
