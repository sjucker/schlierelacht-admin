package ch.schlierelacht.admin.util;

import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

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
}
