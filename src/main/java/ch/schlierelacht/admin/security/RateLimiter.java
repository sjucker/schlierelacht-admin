package ch.schlierelacht.admin.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * In-memory fixed-window request counter: at most {@link Limit#max()} acquisitions per key within
 * {@link Limit#window()}. Counts live in this JVM only — with several instances each one allows the limit.
 * <p>
 * Expired windows are purged once the map grows past {@link #PURGE_THRESHOLD}, so a flood from many addresses
 * cannot grow it without bound.
 */
public class RateLimiter {

    static final int PURGE_THRESHOLD = 10_000;

    public record Limit(int max, Duration window) {
    }

    private record Window(long endNanos, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final LongSupplier nanoTime;

    public RateLimiter() {
        this(System::nanoTime);
    }

    RateLimiter(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    /**
     * Counts one request for {@code key}.
     *
     * @return the seconds until the key's window ends when the limit is exceeded, {@code 0} when the request is allowed
     */
    public long tryAcquire(String key, Limit limit) {
        long now = nanoTime.getAsLong();
        if (windows.size() > PURGE_THRESHOLD) {
            windows.values().removeIf(window -> window.endNanos() - now <= 0);
        }
        Window window = windows.compute(key, (_, current) -> current == null || current.endNanos() - now <= 0
                ? new Window(now + limit.window().toNanos(), 1)
                : new Window(current.endNanos(), Math.min(current.count() + 1, limit.max() + 1)));
        if (window.count() <= limit.max()) {
            return 0;
        }
        return Math.max(1, Duration.ofNanos(window.endNanos() - now).toSeconds());
    }

    int size() {
        return windows.size();
    }
}
