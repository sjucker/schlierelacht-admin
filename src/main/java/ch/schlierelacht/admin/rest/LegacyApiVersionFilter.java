package ch.schlierelacht.admin.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Transitional support for the unversioned {@code /api/<resource>} paths that
 * existed before the public API was versioned.
 *
 * <p>Clients deployed before versioning — the production website and any
 * released build of the mobile app — still call e.g. {@code /api/news}. Those
 * requests are forwarded to {@code /api/v1/news}, which is by definition the
 * shape they were written against. Every forwarded call is logged at WARN so
 * the remaining legacy traffic is visible in the logs.
 *
 * <p><strong>This class is meant to be deleted</strong> once both website
 * branches and every released app build call versioned paths — at that point
 * the WARN log falls silent and dropping the class turns the old paths into
 * plain 404s.
 */
@Slf4j
@Component
public class LegacyApiVersionFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api";
    /** Unversioned paths predate versioning, so they map to the first version. */
    private static final String LEGACY_VERSION = "/v1";
    private static final Pattern VERSIONED_PATH = Pattern.compile("^/api/v\\d+(/.*)?$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var path = request.getRequestURI().substring(request.getContextPath().length());

        if (!path.startsWith(API_PREFIX + "/") || VERSIONED_PATH.matcher(path).matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        var target = API_PREFIX + LEGACY_VERSION + path.substring(API_PREFIX.length());
        log.warn("Deprecated unversioned API call: {} {} -> forwarding to {}", request.getMethod(), path, target);

        request.getRequestDispatcher(target).forward(request, response);
    }
}
