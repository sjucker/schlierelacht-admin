package ch.schlierelacht.admin.dto;

import ch.schlierelacht.admin.mapper.EnumMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum AttractionType implements HasDescription {
    ARTIST("Künstler"),
    FOOD("Essen"),
    EXHIBITION("Ausstellung"),
    DISCUSSION("Diskussion"),
    EVENT("Anlass"),
    RIDE("Bahn");

    private static final EnumMapper ENUM_MAPPER = EnumMapper.INSTANCE;

    private final String description;

    public static Optional<AttractionType> fromDb(ch.schlierelacht.admin.jooq.enums.AttractionType dbEnum) {
        return Optional.ofNullable(ENUM_MAPPER.fromDb(dbEnum));
    }

    public ch.schlierelacht.admin.jooq.enums.AttractionType toDb() {
        return ENUM_MAPPER.toDb(this);
    }
}
