package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LocationDTO(@NotNull String externalId,
                          @NotNull LocationType type,
                          @NotNull String name,
                          @NotNull BigDecimal latitude,
                          @NotNull BigDecimal longitude,
                          @NotNull String googleMapsUrl,
                          String cloudflareId,
                          String mapId,
                          boolean showInFestplan) {
}
