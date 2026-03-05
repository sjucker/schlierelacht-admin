package ch.schlierelacht.admin.dto;

import ch.schlierelacht.admin.mapper.EnumMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum SponsoringType implements HasDescription {
    HAUPTSPONSOREN("Hauptsponsoren"),
    ORGANISATION("Organisation, Veranstalter und Medienpartner"),
    GASTREGION("Gastregion/-stadt"),
    GOLD("Goldsponsoren"),
    SILBER("Silbersponsoren"),
    BRONZE("Bronzesponsoren"),
    GOENNER("Gönner");

    private final String description;

    private static final EnumMapper ENUM_MAPPER = EnumMapper.INSTANCE;

    public static Optional<SponsoringType> fromDb(ch.schlierelacht.admin.jooq.enums.SponsoringType dbEnum) {
        return Optional.ofNullable(ENUM_MAPPER.fromDb(dbEnum));
    }

    public ch.schlierelacht.admin.jooq.enums.SponsoringType toDb() {
        return ENUM_MAPPER.toDb(this);
    }
}
