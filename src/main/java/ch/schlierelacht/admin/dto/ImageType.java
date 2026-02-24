package ch.schlierelacht.admin.dto;

import ch.schlierelacht.admin.mapper.EnumMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum ImageType implements HasDescription {
    MAIN("Hauptbild"),
    ADDITIONAL("Zusatzbild");

    private final String description;

    private static final EnumMapper ENUM_MAPPER = EnumMapper.INSTANCE;

    public static Optional<ImageType> fromDb(ch.schlierelacht.admin.jooq.enums.ImageType dbEnum) {
        return Optional.ofNullable(ENUM_MAPPER.fromDb(dbEnum));
    }

    public ch.schlierelacht.admin.jooq.enums.ImageType toDb() {
        return ENUM_MAPPER.toDb(this);
    }
}
