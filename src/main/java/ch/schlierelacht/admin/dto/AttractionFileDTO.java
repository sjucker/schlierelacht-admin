package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AttractionFileDTO(@NotNull Long id,
                                @NotNull String filename,
                                @NotNull String filetype,
                                @NotNull String description,
                                @NotNull long filesize) {
}
