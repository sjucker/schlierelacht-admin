package ch.schlierelacht.admin.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyApiVersionFilterTest {

    private final LegacyApiVersionFilter filter = new LegacyApiVersionFilter();

    @ParameterizedTest
    @CsvSource({
            "/api/news,                          /api/v1/news",
            "/api/news/42,                       /api/v1/news/42",
            "/api/attraction,                    /api/v1/attraction",
            "/api/attraction/abc/files/7,        /api/v1/attraction/abc/files/7",
            "/api/push/register,                 /api/v1/push/register",
            "/api/sponsoring/type,               /api/v1/sponsoring/type"
    })
    @DisplayName("unversioned API calls are forwarded to their v1 counterpart")
    void forwardsUnversionedCallsToV1(String requested, String expectedTarget) throws Exception {
        var response = doFilter(requested);

        assertThat(response.getForwardedUrl()).as("forward target for %s", requested).isEqualTo(expectedTarget);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/news", "/api/v1/attraction/abc", "/api/v2/news", "/api/v10/news"})
    @DisplayName("already versioned API calls are passed through untouched")
    void passesVersionedCallsThrough(String requested) throws Exception {
        var response = doFilter(requested);

        assertThat(response.getForwardedUrl()).as("forward target for %s", requested).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api", "/login", "/actuator/health", "/images/logo.svg"})
    @DisplayName("non-API calls are passed through untouched")
    void passesNonApiCallsThrough(String requested) throws Exception {
        var response = doFilter(requested);

        assertThat(response.getForwardedUrl()).as("forward target for %s", requested).isNull();
    }

    @Test
    @DisplayName("the forward target is context-relative, so a context path is not doubled")
    void stripsTheContextPath() throws Exception {
        var request = new MockHttpServletRequest("GET", "/admin/api/news");
        request.setContextPath("/admin");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getForwardedUrl()).isEqualTo("/api/v1/news");
    }

    private MockHttpServletResponse doFilter(String requestUri) throws Exception {
        var request = new MockHttpServletRequest("GET", requestUri);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        return response;
    }
}
