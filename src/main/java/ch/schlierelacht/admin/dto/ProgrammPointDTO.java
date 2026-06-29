package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A single programm entry joined with the attraction it belongs to. Returned by
 * {@code GET /api/programm} as a flat, chronologically ordered schedule so
 * consumers no longer have to assemble it client-side from {@code /api/attraction}.
 */
public record ProgrammPointDTO(@NotNull AttractionRefDTO attraction,
                               @NotNull ProgrammEntryDTO entry) {
}
