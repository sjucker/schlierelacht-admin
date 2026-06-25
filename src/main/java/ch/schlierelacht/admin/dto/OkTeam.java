package ch.schlierelacht.admin.dto;

import ch.schlierelacht.admin.mapper.EnumMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum OkTeam implements HasDescription {
    PRAESIDIALES_STADT_KOMMUNIKATION_FINANZEN("Präsidiales/Stadt/Kommunikation/Finanzen"),
    BAU_INFRASTRUKTUR("Bau / Infrastruktur"),
    GASTRONOMIE("Gastronomie"),
    PROGRAMM_AKTIVITAETEN("Programm / Aktivitäten"),
    SICHERHEIT("Sicherheit"),
    SPONSORING("Sponsoring");

    private final String description;

    private static final EnumMapper ENUM_MAPPER = EnumMapper.INSTANCE;

    public static Optional<OkTeam> fromDb(ch.schlierelacht.admin.jooq.enums.OkTeam dbEnum) {
        return Optional.ofNullable(ENUM_MAPPER.fromDb(dbEnum));
    }

    public ch.schlierelacht.admin.jooq.enums.OkTeam toDb() {
        return ENUM_MAPPER.toDb(this);
    }
}
