package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record ImageDTO(@NotNull String cloudflareId,
                       @NotNull String description,
                       @NotNull ImageType type) {
}
