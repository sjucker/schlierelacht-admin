package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ArtistDTO(@NotNull String externalId,
                        @NotNull String name,
                        @NotNull String description,
                        String website,
                        String instagram,
                        String facebook,
                        String youtube,
                        @NotNull List<ImageDTO> images,
                        @NotNull List<TagDTO> tags,
                        @NotNull List<ProgrammEntryDTO> programm) {
}
