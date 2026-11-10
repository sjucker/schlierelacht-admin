package ch.schlierelacht.admin.dto;

import ch.schlierelacht.admin.mapper.EnumMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * How important a {@link MessageDTO} is. Declared from most to least important: the app shows the active message
 * with the highest priority on its home screen.
 */
@Getter
@RequiredArgsConstructor
public enum MessagePriority implements HasDescription {
    HIGH("Notfall"),
    MEDIUM("Warnung"),
    LOW("Info");

    private final String description;

    private static final EnumMapper ENUM_MAPPER = EnumMapper.INSTANCE;

    public static Optional<MessagePriority> fromDb(ch.schlierelacht.admin.jooq.enums.MessagePriority dbEnum) {
        return Optional.ofNullable(ENUM_MAPPER.fromDb(dbEnum));
    }

    public ch.schlierelacht.admin.jooq.enums.MessagePriority toDb() {
        return ENUM_MAPPER.toDb(this);
    }
}
