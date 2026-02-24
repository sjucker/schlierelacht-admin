package ch.schlierelacht.admin.dto;

import ch.schlierelacht.admin.mapper.EnumMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum LocationType implements HasDescription {
    STAGE("Bühne"),
    FOOD_STAND("Essensstand"),
    BAR("Bar"),
    TENT("Festzelt"),
    ATTRACTION("Attraktion");

    private final String description;

    private static final EnumMapper ENUM_MAPPER = EnumMapper.INSTANCE;

    public static Optional<LocationType> fromDb(ch.schlierelacht.admin.jooq.enums.LocationType dbEnum) {
        return Optional.ofNullable(ENUM_MAPPER.fromDb(dbEnum));
    }

    public ch.schlierelacht.admin.jooq.enums.LocationType toDb() {
        return ENUM_MAPPER.toDb(this);
    }
}
