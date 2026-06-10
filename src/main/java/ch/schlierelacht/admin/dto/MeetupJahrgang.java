package ch.schlierelacht.admin.dto;

import ch.schlierelacht.admin.mapper.EnumMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum MeetupJahrgang implements HasDescription {
    BEFORE_1954("1953 + älter – Mo, 6.9.2027, 17:30"),
    BORN_1954_1958("1954–1958 – Di, 7.9.2027, 17:30"),
    BORN_1959_1963("1959–1963 – Mo, 6.9.2027, 17:30"),
    BORN_1965_1968("1965–1968 – Di, 7.9.2027, 17:30"),
    BORN_1969_1973("1969–1973 – Mi, 8.9.2027, 17:30"),
    BORN_1974_1978("1974–1978 – Do, 9.9.2027, 17:30"),
    BORN_1979_1983("1979–1983 – Mi, 8.9.2027, 17:30"),
    BORN_1984_1988("1984–1988 – Do, 9.9.2027, 17:30"),
    BORN_1989_1993("1989–1993 – Fr, 10.9.2027, 17:30"),
    BORN_1994_1998("1994–1998 – Fr, 10.9.2027, 17:30"),
    AFTER_1998("1999 + jünger – Fr, 10.9.2027, 17:30");

    private final String description;

    private static final EnumMapper ENUM_MAPPER = EnumMapper.INSTANCE;

    public static Optional<MeetupJahrgang> fromDb(ch.schlierelacht.admin.jooq.enums.MeetupJahrgang dbEnum) {
        return Optional.ofNullable(ENUM_MAPPER.fromDb(dbEnum));
    }

    public ch.schlierelacht.admin.jooq.enums.MeetupJahrgang toDb() {
        return ENUM_MAPPER.toDb(this);
    }
}
