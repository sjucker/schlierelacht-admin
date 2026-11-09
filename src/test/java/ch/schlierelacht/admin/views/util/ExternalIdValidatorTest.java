package ch.schlierelacht.admin.views.util;

import com.vaadin.flow.data.binder.ValueContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalIdValidatorTest {

    private final ExternalIdValidator validator = new ExternalIdValidator();

    @ParameterizedTest
    @ValueSource(strings = {"dj-mario", "12", "A1", "live-im-park-buehne"})
    void valid(String externalId) {
        assertThat(validator.apply(externalId, new ValueContext()).isError()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dj mario", " dj-mario", "dj-mario ", "dj/mario", "bühne", "dj?mario", "dj#mario", "dj.mario", "live_im_park"})
    void invalid(String externalId) {
        assertThat(validator.apply(externalId, new ValueContext()).isError()).isTrue();
    }
}
