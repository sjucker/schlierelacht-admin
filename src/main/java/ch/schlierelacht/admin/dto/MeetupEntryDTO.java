package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record MeetupEntryDTO(@NotNull String firstname,
                             @NotNull String lastname,
                             @NotNull MeetupJahrgang jahrgang) {
}
