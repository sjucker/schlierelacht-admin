package ch.schlierelacht.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiWriteGuardFilterTest {

    private static final String PROBLEM_REPORT = "/api/v1/problem-report";
    private static final String MEETUP = "/api/v1/meetup";

    private final ApiWriteGuardFilter filter = new ApiWriteGuardFilter(new RateLimiter());

    @Test
    void passes_a_write_within_the_limits() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(post(PROBLEM_REPORT, "203.0.113.1", 1000), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejects_a_client_that_sends_too_many_writes() throws Exception {
        int max = ApiWriteGuardFilter.RULES.getFirst().perClient().max();
        for (int i = 0; i < max; i++) {
            assertThat(send(post(PROBLEM_REPORT, "203.0.113.1", 1000)).getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse rejected = send(post(PROBLEM_REPORT, "203.0.113.1", 1000));

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(rejected.getHeader("Retry-After"))).isBetween(1L, 600L);
        // another client and another resource are not affected
        assertThat(send(post(PROBLEM_REPORT, "203.0.113.2", 1000)).getStatus()).isEqualTo(200);
        assertThat(send(post(MEETUP, "203.0.113.1", 1000)).getStatus()).isEqualTo(200);
    }

    @Test
    void rejects_everyone_once_the_overall_limit_is_reached() throws Exception {
        int max = ApiWriteGuardFilter.RULES.getFirst().overall().max();
        for (int i = 0; i < max; i++) {
            assertThat(send(post(PROBLEM_REPORT, "198.51.100." + i, 1000)).getStatus()).isEqualTo(200);
        }

        assertThat(send(post(PROBLEM_REPORT, "203.0.113.1", 1000)).getStatus()).isEqualTo(429);
    }

    @Test
    void does_not_limit_reads() throws Exception {
        for (int i = 0; i < 500; i++) {
            MockHttpServletRequest get = new MockHttpServletRequest("GET", MEETUP);
            assertThat(send(get).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void rejects_a_body_that_is_too_large_for_the_resource() throws Exception {
        assertThat(send(post(PROBLEM_REPORT, "203.0.113.1", 11 * ApiWriteGuardFilter.MB)).getStatus()).isEqualTo(200);
        assertThat(send(post(PROBLEM_REPORT, "203.0.113.1", 13 * ApiWriteGuardFilter.MB)).getStatus()).isEqualTo(413);
        assertThat(send(post(MEETUP, "203.0.113.1", 65 * ApiWriteGuardFilter.KB)).getStatus()).isEqualTo(413);
        assertThat(send(post("/api/v1/unknown", "203.0.113.1", 65 * ApiWriteGuardFilter.KB)).getStatus()).isEqualTo(413);
    }

    @Test
    void rejects_a_body_of_undeclared_length() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", MEETUP);

        assertThat(send(request).getStatus()).isEqualTo(411);
    }

    @Test
    void takes_the_client_from_the_last_forwarded_address() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", MEETUP);
        request.setRemoteAddr("10.1.2.3");
        assertThat(ApiWriteGuardFilter.clientKey(request)).isEqualTo("10.1.2.3");

        // the first entry is whatever the client sent, the last one is appended by the router
        request.addHeader("X-Forwarded-For", "1.1.1.1, 203.0.113.7");
        assertThat(ApiWriteGuardFilter.clientKey(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void counts_ipv6_clients_per_64_block() {
        assertThat(clientKey("2001:db8:1:2:aaaa:bbbb:cccc:dddd"))
                .isEqualTo(clientKey("2001:db8:1:2::1"))
                .isNotEqualTo(clientKey("2001:db8:1:3::1"));
    }

    @Test
    void counts_unparseable_addresses_together() {
        assertThat(clientKey("not-an-address")).isEqualTo(clientKey("example.com")).isEqualTo("unknown");
    }

    private static String clientKey(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", MEETUP);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return ApiWriteGuardFilter.clientKey(request);
    }

    private static MockHttpServletRequest post(String uri, String client, long contentLength) {
        // the mock derives its length from the content; declare one without allocating megabytes
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri) {
            @Override
            public long getContentLengthLong() {
                return contentLength;
            }
        };
        request.addHeader("X-Forwarded-For", client);
        return request;
    }

    private MockHttpServletResponse send(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
