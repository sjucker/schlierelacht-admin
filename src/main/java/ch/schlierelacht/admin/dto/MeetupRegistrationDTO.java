package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record MeetupRegistrationDTO(@NotNull String firstname,
                                    @NotNull String lastname,
                                    @NotNull String email,
                                    @NotNull MeetupJahrgang jahrgang,
                                    @NotNull boolean showOnList) {
}
