package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AttractionDTO(@NotNull String externalId,
                            @NotNull String name,
                            @NotNull String description,
                            String website,
                            String instagram,
                            String facebook,
                            String youtube,
                            String operator,
                            @NotNull List<ImageDTO> images,
                            @NotNull List<ProgrammEntryDTO> programm,
                            @NotNull List<AttractionFileDTO> files) {
}
