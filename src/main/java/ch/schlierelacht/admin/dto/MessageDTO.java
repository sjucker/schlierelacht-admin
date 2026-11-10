package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * A Meldung for the app: an emergency alert, a warning or an info. {@code content} is markdown.
 */
public record MessageDTO(@NotNull Long id,
                         @NotNull MessagePriority priority,
                         @NotNull String title,
                         @NotNull String content,
                         @NotNull LocalDateTime createdAt) {
}
