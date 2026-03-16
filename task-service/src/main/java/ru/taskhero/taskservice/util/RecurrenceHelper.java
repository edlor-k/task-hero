package ru.taskhero.taskservice.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Утилита для работы с правилами повторения в формате RRULE (iCalendar RFC 5545).
 * <p>
 * Поддерживаемые правила:
 * <ul>
 *     <li>FREQ=DAILY — ежедневно</li>
 *     <li>FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR — каждый будний день</li>
 *     <li>FREQ=WEEKLY;BYDAY=MO,WE,FR — каждую неделю по определённым дням</li>
 *     <li>FREQ=WEEKLY;INTERVAL=2;BYDAY=TU — каждые 2 недели по вторникам</li>
 *     <li>FREQ=MONTHLY;BYMONTHDAY=15 — каждый месяц 15-го числа</li>
 *     <li>FREQ=MONTHLY;INTERVAL=2 — раз в 2 месяца</li>
 * </ul>
 */
public final class RecurrenceHelper {

    private RecurrenceHelper() {
    }

    /**
     * Вычислить следующие N дат по RRULE начиная от указанной даты.
     *
     * @param rrule     правило повторения
     * @param startDate дата начала
     * @param count     количество дат
     * @return список дат
     */
    public static List<LocalDate> getNextOccurrences(String rrule, LocalDate startDate, int count) {
        if (rrule == null || rrule.isBlank()) {
            return List.of();
        }

        String freq = extractParam(rrule, "FREQ");
        int interval = parseIntParam(rrule, "INTERVAL", 1);
        Set<DayOfWeek> byDay = parseBYDAY(rrule);
        int byMonthDay = parseIntParam(rrule, "BYMONTHDAY", -1);

        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;

        // Максимальный лимит итераций для безопасности
        int maxIterations = count * 400;
        int iterations = 0;

        while (dates.size() < count && iterations < maxIterations) {
            iterations++;

            switch (freq) {
                case "DAILY" -> {
                    if (byDay.isEmpty() || byDay.contains(current.getDayOfWeek())) {
                        dates.add(current);
                    }
                    current = current.plusDays(byDay.isEmpty() ? interval : 1);
                }
                case "WEEKLY" -> {
                    if (byDay.isEmpty()) {
                        dates.add(current);
                        current = current.plusWeeks(interval);
                    } else {
                        if (byDay.contains(current.getDayOfWeek())) {
                            dates.add(current);
                        }
                        // Переход к следующему дню, учитывая интервал недель
                        LocalDate next = current.plusDays(1);
                        if (next.getDayOfWeek() == DayOfWeek.MONDAY
                                && !current.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
                            // Начало новой недели — учитываем интервал
                            next = current.plusWeeks(interval)
                                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                        } else if (current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                            next = current.plusWeeks(interval - 1)
                                    .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                        }
                        current = next;
                    }
                }
                case "MONTHLY" -> {
                    if (byMonthDay > 0) {
                        try {
                            LocalDate candidate = current.withDayOfMonth(
                                    Math.min(byMonthDay, current.lengthOfMonth()));
                            if (!candidate.isBefore(startDate)) {
                                dates.add(candidate);
                            }
                        } catch (Exception ignored) {
                            // Если дата недопустима для месяца
                        }
                    } else {
                        dates.add(current);
                    }
                    current = current.plusMonths(interval);
                }
                default -> {
                    return dates;
                }
            }
        }

        return dates;
    }

    /**
     * Проверить валидность RRULE.
     */
    public static boolean isValid(String rrule) {
        if (rrule == null || rrule.isBlank()) {
            return false;
        }
        String freq = extractParam(rrule, "FREQ");
        return freq != null && (freq.equals("DAILY") || freq.equals("WEEKLY") || freq.equals("MONTHLY"));
    }

    /**
     * Сгенерировать описание RRULE на русском языке.
     */
    public static String toHumanReadable(String rrule) {
        if (rrule == null || rrule.isBlank()) {
            return "Без повторения";
        }

        String freq = extractParam(rrule, "FREQ");
        int interval = parseIntParam(rrule, "INTERVAL", 1);
        Set<DayOfWeek> byDay = parseBYDAY(rrule);
        int byMonthDay = parseIntParam(rrule, "BYMONTHDAY", -1);

        return switch (freq) {
            case "DAILY" -> {
                if (byDay.size() == 5 && byDay.containsAll(
                        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))) {
                    yield "Каждый будний день";
                }
                if (interval == 1) {
                    yield "Ежедневно";
                }
                yield "Каждые " + interval + " дн.";
            }
            case "WEEKLY" -> {
                StringBuilder sb = new StringBuilder();
                if (interval == 1) {
                    sb.append("Еженедельно");
                } else {
                    sb.append("Каждые ").append(interval).append(" нед.");
                }
                if (!byDay.isEmpty()) {
                    sb.append(" по ");
                    sb.append(daysToRussian(byDay));
                }
                yield sb.toString();
            }
            case "MONTHLY" -> {
                if (byMonthDay > 0) {
                    if (interval == 1) {
                        yield "Ежемесячно " + byMonthDay + "-го числа";
                    }
                    yield "Каждые " + interval + " мес. " + byMonthDay + "-го числа";
                }
                if (interval == 1) {
                    yield "Ежемесячно";
                }
                yield "Каждые " + interval + " мес.";
            }
            default -> rrule;
        };
    }

    private static String extractParam(String rrule, String param) {
        for (String part : rrule.split(";")) {
            if (part.startsWith(param + "=")) {
                return part.substring(param.length() + 1);
            }
        }
        return null;
    }

    private static int parseIntParam(String rrule, String param, int defaultValue) {
        String value = extractParam(rrule, param);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Set<DayOfWeek> parseBYDAY(String rrule) {
        String value = extractParam(rrule, "BYDAY");
        if (value == null) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String day : value.split(",")) {
            switch (day.trim()) {
                case "MO" -> days.add(DayOfWeek.MONDAY);
                case "TU" -> days.add(DayOfWeek.TUESDAY);
                case "WE" -> days.add(DayOfWeek.WEDNESDAY);
                case "TH" -> days.add(DayOfWeek.THURSDAY);
                case "FR" -> days.add(DayOfWeek.FRIDAY);
                case "SA" -> days.add(DayOfWeek.SATURDAY);
                case "SU" -> days.add(DayOfWeek.SUNDAY);
                default -> { /* ignore unknown */ }
            }
        }
        return days;
    }

    private static String daysToRussian(Set<DayOfWeek> days) {
        List<String> names = new ArrayList<>();
        if (days.contains(DayOfWeek.MONDAY)) names.add("Пн");
        if (days.contains(DayOfWeek.TUESDAY)) names.add("Вт");
        if (days.contains(DayOfWeek.WEDNESDAY)) names.add("Ср");
        if (days.contains(DayOfWeek.THURSDAY)) names.add("Чт");
        if (days.contains(DayOfWeek.FRIDAY)) names.add("Пт");
        if (days.contains(DayOfWeek.SATURDAY)) names.add("Сб");
        if (days.contains(DayOfWeek.SUNDAY)) names.add("Вс");
        return String.join(", ", names);
    }
}
