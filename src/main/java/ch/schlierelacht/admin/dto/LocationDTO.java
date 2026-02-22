package ch.schlierelacht.admin.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record LocationDTO(@NotNull String externalId,
                          @NotNull LocationType type,
                          @NotNull String name,
                          @NotNull BigDecimal latitude,
                          @NotNull BigDecimal longitude,
                          @NotNull String googleMapsUrl,
                          String cloudflareId,
                          String mapId) {
}
