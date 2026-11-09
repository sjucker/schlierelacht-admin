package ch.schlierelacht.admin.views.util;

import com.vaadin.flow.data.validator.RegexpValidator;

/**
 * Validates an external ID, which the website and the app use as a URL path segment
 * (e.g. {@code /programm/dj-mario}): only letters, digits and '-' are allowed (no '_', for style), so no
 * whitespace, slashes, umlauts or other characters that would need URL-encoding.
 */
public class ExternalIdValidator extends RegexpValidator {

    public static final String PATTERN = "[A-Za-z0-9-]+";

    public ExternalIdValidator() {
        super("Nur Buchstaben (ohne Umlaute), Ziffern und '-' erlaubt, keine Leerzeichen", PATTERN);
    }
}
