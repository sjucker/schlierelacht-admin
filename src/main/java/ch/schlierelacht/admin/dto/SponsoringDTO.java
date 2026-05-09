package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record SponsoringDTO(@NotNull SponsoringType type,
                            @NotNull String name,
                            String cloudflareId,
                            String url) {
}
