package ch.schlierelacht.admin.security;

import ch.schlierelacht.admin.AbstractIntegrationTest;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTest extends AbstractIntegrationTest {

    private final FilterChainProxy filterChainProxy;

    @Autowired
    SecurityConfigurationTest(FilterChainProxy filterChainProxy) {
        this.filterChainProxy = filterChainProxy;
    }

    @Test
    void public_api_is_guarded_against_write_floods_after_cors() {
        List<Class<?>> filters = filterChainProxy.getFilters("/api/v1/problem-report").stream().<Class<?>>map(Filter::getClass).toList();

        assertThat(filters).contains(ApiWriteGuardFilter.class);
        assertThat(filters.indexOf(ApiWriteGuardFilter.class)).isGreaterThan(filters.indexOf(CorsFilter.class));
    }

    @Test
    void admin_ui_is_not_rate_limited() {
        assertThat(filterChainProxy.getFilters("/login")).noneMatch(ApiWriteGuardFilter.class::isInstance);
    }
}
