package ch.schlierelacht.admin.util;

import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static java.util.Locale.GERMAN;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class DateUtil {

    public static final String ZURICH = "Europe/Zurich";

    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of(ZURICH));
    }

    public static LocalDate today() {
        return LocalDate.now(ZoneId.of(ZURICH));
    }

    public static LocalTime currentTime() {
        return LocalTime.now(ZoneId.of(ZURICH));
    }

    public static String formatInstant(Instant instant) {
        if (instant == null) {
            return "";
        }
        return formatDateTime(instant.atZone(ZoneId.of(ZURICH)).toLocalDateTime());
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", GERMAN));
    }
}
