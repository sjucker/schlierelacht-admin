package ch.schlierelacht.admin.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record ArtistDTO(@NotNull String externalId,
                        @NotNull String name,
                        @NotNull String description,
                        String website,
                        String instagram,
                        String facebook,
                        String youtube,
                        @NotNull List<ImageDTO> images,
                        @NotNull List<ProgrammEntryDTO> programm) {
}
