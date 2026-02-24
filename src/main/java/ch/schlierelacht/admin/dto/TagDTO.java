package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record TagDTO(@NotNull Long id,
                     @NotNull String name) {
}
