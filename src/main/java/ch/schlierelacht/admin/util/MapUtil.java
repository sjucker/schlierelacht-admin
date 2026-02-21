package ch.schlierelacht.admin.util;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class MapUtil {

    public static String getGoogleMapsCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return "https://www.google.ch/maps?q=%s,%s".formatted(latitude, longitude);
    }
}
