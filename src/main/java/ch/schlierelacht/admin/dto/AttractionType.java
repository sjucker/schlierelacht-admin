package ch.schlierelacht.admin.dto;

import ch.schlierelacht.admin.mapper.EnumMapper;

public enum AttractionType {
    ARTIST,
    FOOD,
    EXHIBITION,
    DISCUSSION,
    EVENT,
    RIDE;

    private static final EnumMapper ENUM_MAPPER = EnumMapper.INSTANCE;

    public static AttractionType fromDb(ch.schlierelacht.admin.jooq.enums.AttractionType dbEnum) {
        return ENUM_MAPPER.fromDb(dbEnum);
    }

    public ch.schlierelacht.admin.jooq.enums.AttractionType toDb() {
        return ENUM_MAPPER.toDb(this);
    }
}
