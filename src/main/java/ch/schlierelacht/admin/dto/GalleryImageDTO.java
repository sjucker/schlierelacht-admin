package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record GalleryImageDTO(@NotNull String cloudflareId) {
}
