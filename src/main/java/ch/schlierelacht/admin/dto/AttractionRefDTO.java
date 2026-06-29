package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Lightweight reference to an attraction, carrying just enough to label and link
 * a programm point. See {@link ProgrammPointDTO}.
 */
public record AttractionRefDTO(@NotNull String externalId,
                               @NotNull String name) {
}
