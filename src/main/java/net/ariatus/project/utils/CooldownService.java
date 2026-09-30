package net.ariatus.project.utils;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class CooldownService<K> {

    private final Map<K, Long> expirations =
            new ConcurrentHashMap<>();

    public void set(
            K key,
            Duration duration
    ) {
        Objects.requireNonNull(
                key,
                "key"
        );

        Objects.requireNonNull(
                duration,
                "duration"
        );

        if (
                duration.isZero()
                        || duration.isNegative()
        ) {
            clear(
                    key
            );

            return;
        }

        long durationNanos;

        try {
            durationNanos =
                    duration.toNanos();

        } catch (ArithmeticException exception) {
            durationNanos =
                    Long.MAX_VALUE;
        }

        long now =
                System.nanoTime();

        long expiration =
                durationNanos
                        >= Long.MAX_VALUE - now
                        ? Long.MAX_VALUE
                        : now + durationNanos;

        expirations.put(
                key,
                expiration
        );
    }

    public void setSeconds(
            K key,
            long seconds
    ) {
        set(
                key,
                Duration.ofSeconds(
                        Math.max(
                                0L,
                                seconds
                        )
                )
        );
    }

    public void setMillis(
            K key,
            long millis
    ) {
        set(
                key,
                Duration.ofMillis(
                        Math.max(
                                0L,
                                millis
                        )
                )
        );
    }

    public boolean active(
            K key
    ) {
        return remainingNanos(
                key
        ) > 0L;
    }

    public Duration remaining(
            K key
    ) {
        long remaining =
                remainingNanos(
                        key
                );

        return remaining <= 0L
                ? Duration.ZERO
                : Duration.ofNanos(
                remaining
        );
    }

    public long remainingMillis(
            K key
    ) {
        return TimeUnit.NANOSECONDS
                .toMillis(
                        remainingNanos(
                                key
                        )
                );
    }

    public long remainingSeconds(
            K key
    ) {
        return TimeUnit.NANOSECONDS
                .toSeconds(
                        remainingNanos(
                                key
                        )
                );
    }

    public void clear(
            K key
    ) {
        if (key == null) return;

        expirations.remove(
                key
        );
    }

    public void clearAll() {
        expirations.clear();
    }

    public int cleanup() {
        long now =
                System.nanoTime();

        int before =
                expirations.size();

        expirations.entrySet()
                .removeIf(
                        entry ->
                                now
                                        >= entry.getValue()
                );

        return before
                - expirations.size();
    }

    public int size() {
        cleanup();
        return expirations.size();
    }

    private long remainingNanos(
            K key
    ) {
        if (key == null) return 0L;

        Long expiration =
                expirations.get(
                        key
                );

        if (expiration == null) {
            return 0L;
        }

        long remaining =
                expiration
                        - System.nanoTime();

        if (remaining <= 0L) {
            expirations.remove(
                    key,
                    expiration
            );

            return 0L;
        }

        return remaining;
    }
}