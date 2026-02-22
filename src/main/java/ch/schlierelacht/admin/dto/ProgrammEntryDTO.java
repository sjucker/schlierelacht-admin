package ch.schlierelacht.admin.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record ProgrammEntryDTO(@NotNull LocationDTO location,
                               @NotNull LocalDate fromDate,
                               LocalTime fromTime,
                               LocalDate toDate,
                               LocalTime toTime) {
}
