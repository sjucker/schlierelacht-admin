package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GalleryCategoryDTO(@NotNull String category,
                                 @NotNull List<GalleryImageDTO> images) {
}
