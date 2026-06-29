package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ProgrammEntryDTO(@NotNull LocationDTO location,
                               @NotNull LocalDate fromDate,
                               LocalTime fromTime,
                               LocalDate toDate,
                               LocalTime toTime,
                               boolean past) {

    /**
     * Whether the entry is completely in the past relative to {@code now}:
     * <ul>
     *   <li>with an end (validTo): once that end has passed — using {@code toTime} when present,
     *       otherwise the end of the {@code toDate} day (i.e. the following day has begun);</li>
     *   <li>with only a start (validFrom): once the day after {@code fromDate} has begun.</li>
     * </ul>
     */
    public static boolean isPast(LocalDate fromDate, LocalDate toDate, LocalTime toTime, LocalDateTime now) {
        if (toDate != null) {
            LocalDateTime end = toTime != null ? toDate.atTime(toTime) : toDate.plusDays(1).atStartOfDay();
            return !now.isBefore(end);
        }
        return !now.isBefore(fromDate.plusDays(1).atStartOfDay());
    }
}
