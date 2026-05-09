package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record SponsoringTypeDTO(@NotNull SponsoringType type,
                                @NotNull String description) {

    public static SponsoringTypeDTO of(SponsoringType type) {
        return new SponsoringTypeDTO(type, type.getDescription());
    }
}
