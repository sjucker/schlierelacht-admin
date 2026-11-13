package ch.schlierelacht.admin.security;

import ch.schlierelacht.admin.security.RateLimiter.Limit;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Keeps the public API's write requests ({@code POST /api/**}) from being used to flood the backend, the mailbox of
 * the OK or the database. The API is unauthenticated, so every write is limited before its body is read:
 * <ul>
 *     <li><b>per client</b> (IP address) — stops a single sender; answered with 429 and {@code Retry-After}</li>
 *     <li><b>overall</b> per resource — caps what a distributed flood can cause (mails sent, rows written)</li>
 *     <li><b>body size</b> — the declared {@code Content-Length} must fit the resource (413), and must be present
 *     (411), so nobody can make the server buffer a 100 MB upload</li>
 * </ul>
 * The rules live in {@link #RULES}; a POST that matches none gets {@link #DEFAULT_RULE}. Read requests are not
 * limited here.
 */
@Slf4j
public class ApiWriteGuardFilter extends OncePerRequestFilter {

    /**
     * @param path          the resource, matched against the request URI in any API version
     * @param perClient     limit per client address; generous, because visitors on the same mobile carrier or Wi-Fi
     *                      share an address
     * @param overall       limit over all clients, {@code null} for none
     * @param maxBodyBytes  largest accepted {@code Content-Length}
     */
    record Rule(String name, Pattern path, Limit perClient, Limit overall, long maxBodyBytes) {
    }

    static final long KB = 1024;
    static final long MB = 1024 * KB;

    static final List<Rule> RULES = List.of(
            // mails the OK; the photo may be 10 MB (ProblemReportService.MAX_PHOTO_SIZE_BYTES)
            new Rule("problem-report", Pattern.compile("/api/v\\d+/problem-report"),
                     new Limit(10, Duration.ofMinutes(10)), new Limit(100, Duration.ofHours(1)), 12 * MB),
            // mails the registrant and the OK, writes a row
            new Rule("meetup", Pattern.compile("/api/v\\d+/meetup"),
                     new Limit(10, Duration.ofMinutes(10)), new Limit(200, Duration.ofHours(1)), 64 * KB),
            // every app launch re-registers its token (an upsert)
            new Rule("push-register", Pattern.compile("/api/v\\d+/push/register"),
                     new Limit(30, Duration.ofMinutes(1)), new Limit(600, Duration.ofMinutes(1)), 64 * KB));

    static final Rule DEFAULT_RULE = new Rule("other", Pattern.compile(".*"), new Limit(30, Duration.ofMinutes(1)), null, 64 * KB);

    private final RateLimiter rateLimiter;

    public ApiWriteGuardFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        Rule rule = RULES.stream().filter(r -> r.path().matcher(uri).matches()).findFirst().orElse(DEFAULT_RULE);
        String client = clientKey(request);

        long retryAfter = rateLimiter.tryAcquire(rule.name() + '|' + client, rule.perClient());
        if (retryAfter == 0 && rule.overall() != null) {
            retryAfter = rateLimiter.tryAcquire(rule.name(), rule.overall());
            // ERROR is mailed to the admin (logback-spring.xml): once per window, not once per rejected request
            if (retryAfter > 0 && rateLimiter.tryAcquire("alert|" + rule.name(), new Limit(1, rule.overall().window())) == 0) {
                log.error("Overall limit for POST {} reached ({} per {}) — requests are rejected for {} s",
                          uri, rule.overall().max(), rule.overall().window(), retryAfter);
            }
        }
        if (retryAfter > 0) {
            log.warn("Rejected POST {} from {}: too many requests", uri, client);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            response.sendError(429);
            return;
        }

        long length = request.getContentLengthLong();
        if (length < 0) {
            log.warn("Rejected POST {} from {}: no Content-Length", uri, client);
            response.sendError(HttpServletResponse.SC_LENGTH_REQUIRED);
            return;
        }
        if (length > rule.maxBodyBytes()) {
            log.warn("Rejected POST {} from {}: {} bytes", uri, client, length);
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * The client's address as the key to count by. Behind the Heroku router the socket address is the router's, and
     * the router <i>appends</i> the real client address to {@code X-Forwarded-For} — so only the last entry is
     * trustworthy; anything before it is sent by the client. IPv6 clients are counted per /64, the block a single
     * subscriber gets.
     */
    static String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String address = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.substring(forwardedFor.lastIndexOf(',') + 1).trim();
        try {
            if (InetAddress.ofLiteral(address) instanceof Inet6Address v6) {
                return HexFormat.of().formatHex(Arrays.copyOf(v6.getAddress(), 8)) + "/64";
            }
            return address;
        } catch (IllegalArgumentException _) {
            // not an address: count all such requests together rather than letting them choose their own key
            return "unknown";
        }
    }
}
