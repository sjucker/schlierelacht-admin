package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * The FAQ entries sharing a group name, in display order.
 */
public record FaqGroupDTO(@NotNull String name,
                          @NotNull List<FaqEntryDTO> entries) {
}
