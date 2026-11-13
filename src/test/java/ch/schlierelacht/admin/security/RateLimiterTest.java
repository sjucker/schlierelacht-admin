package ch.schlierelacht.admin.security;

import ch.schlierelacht.admin.security.RateLimiter.Limit;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private static final Limit TWO_PER_MINUTE = new Limit(2, Duration.ofMinutes(1));

    private final AtomicLong now = new AtomicLong(Long.MAX_VALUE - 1000); // nanoTime may overflow
    private final RateLimiter rateLimiter = new RateLimiter(now::get);

    @Test
    void allows_up_to_the_limit_then_reports_the_remaining_window() {
        assertThat(rateLimiter.tryAcquire("a", TWO_PER_MINUTE)).isZero();
        assertThat(rateLimiter.tryAcquire("a", TWO_PER_MINUTE)).isZero();

        advance(Duration.ofSeconds(20));

        assertThat(rateLimiter.tryAcquire("a", TWO_PER_MINUTE)).isEqualTo(40);
    }

    @Test
    void counts_each_key_on_its_own() {
        rateLimiter.tryAcquire("a", TWO_PER_MINUTE);
        rateLimiter.tryAcquire("a", TWO_PER_MINUTE);

        assertThat(rateLimiter.tryAcquire("a", TWO_PER_MINUTE)).isPositive();
        assertThat(rateLimiter.tryAcquire("b", TWO_PER_MINUTE)).isZero();
    }

    @Test
    void allows_again_once_the_window_is_over() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("a", TWO_PER_MINUTE);
        }

        advance(Duration.ofMinutes(1));

        assertThat(rateLimiter.tryAcquire("a", TWO_PER_MINUTE)).isZero();
    }

    @Test
    void purges_expired_windows_when_it_grows_too_large() {
        for (int i = 0; i <= RateLimiter.PURGE_THRESHOLD; i++) {
            rateLimiter.tryAcquire("key" + i, TWO_PER_MINUTE);
        }
        assertThat(rateLimiter.size()).isEqualTo(RateLimiter.PURGE_THRESHOLD + 1);

        advance(Duration.ofMinutes(1));
        rateLimiter.tryAcquire("late", TWO_PER_MINUTE);

        assertThat(rateLimiter.size()).isEqualTo(1);
    }

    private void advance(Duration duration) {
        now.addAndGet(duration.toNanos());
    }
}
