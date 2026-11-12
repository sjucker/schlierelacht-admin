package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * One FAQ question ({@code title}) with its answer ({@code content}, markdown).
 */
public record FaqEntryDTO(@NotNull Long id,
                          @NotNull String title,
                          @NotNull String content) {
}
