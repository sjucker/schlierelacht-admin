package ch.schlierelacht.admin.dto;

import ch.schlierelacht.admin.mapper.EnumMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum DownloadCategory implements HasDescription {
    LOGOS("Logos"),
    SPONSORING("Sponsoring"),
    GASTRONOMIE("Gastronomie"),
    PROGRAMM("Programm");

    private final String description;

    private static final EnumMapper ENUM_MAPPER = EnumMapper.INSTANCE;

    public static Optional<DownloadCategory> fromDb(ch.schlierelacht.admin.jooq.enums.DownloadCategory dbEnum) {
        return Optional.ofNullable(ENUM_MAPPER.fromDb(dbEnum));
    }

    public ch.schlierelacht.admin.jooq.enums.DownloadCategory toDb() {
        return ENUM_MAPPER.toDb(this);
    }
}
