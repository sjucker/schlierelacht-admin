package ch.schlierelacht.admin.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record NewsDTO(@NotNull Long id,
                      @NotNull LocalDate date,
                      @NotNull String title,
                      @NotNull String introText,
                      @NotNull String fullText,
                      String cloudflareId) {
}
