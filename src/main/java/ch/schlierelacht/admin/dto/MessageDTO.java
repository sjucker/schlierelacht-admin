package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * A Meldung for the app: an emergency alert, a warning or an info. {@code content} is markdown.
 * {@code publishedAt} is when it was last activated — what the app shows, since a message can be prepared in advance
 * and published later; {@code createdAt} is when it was written.
 */
public record MessageDTO(@NotNull Long id,
                         @NotNull MessagePriority priority,
                         @NotNull String title,
                         @NotNull String content,
                         @NotNull LocalDateTime createdAt,
                         @NotNull LocalDateTime publishedAt) {
}
