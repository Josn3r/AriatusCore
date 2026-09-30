package net.ariatus.project.utils;

import java.time.Duration;

public final class TimeUtils {

    public static final long TICKS_PER_SECOND = 20L;
    public static final long TICKS_PER_MINUTE = TICKS_PER_SECOND * 60L;
    public static final long TICKS_PER_HOUR = TICKS_PER_MINUTE * 60L;

    private TimeUtils() {
    }

    public static String formatHms(long totalSeconds) {
        long safeSeconds = Math.max(0L, totalSeconds);

        long hours = safeSeconds / 3600L;
        long minutes = (safeSeconds % 3600L) / 60L;
        long seconds = safeSeconds % 60L;

        return String.format(
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
        );
    }

    public static String formatShortTime(long totalSeconds) {
        long safeSeconds = Math.max(0L, totalSeconds);

        long days = safeSeconds / 86400L;
        long hours = (safeSeconds % 86400L) / 3600L;
        long minutes = (safeSeconds % 3600L) / 60L;
        long seconds = safeSeconds % 60L;

        if (days > 0L) return days + "d " + hours + "h";
        if (hours > 0L) return hours + "h " + minutes + "m";
        if (minutes > 0L) return minutes + "m " + seconds + "s";

        return seconds + "s";
    }

    public static String formatDuration(Duration duration) {
        if (duration == null) return "00:00:00";
        return formatHms(duration.toSeconds());
    }

    public static boolean hasCooldown(
            long lastUseMillis,
            long cooldownMillis
    ) {
        return remainingCooldownMillis(
                lastUseMillis,
                cooldownMillis
        ) > 0L;
    }

    public static long remainingCooldownMillis(
            long lastUseMillis,
            long cooldownMillis
    ) {
        if (
                lastUseMillis <= 0L
                        || cooldownMillis <= 0L
        ) {
            return 0L;
        }

        long elapsed =
                System.currentTimeMillis()
                        - lastUseMillis;

        return Math.max(
                0L,
                cooldownMillis - elapsed
        );
    }

    public static String formatCooldown(
            long lastUseMillis,
            long cooldownMillis
    ) {
        long remainingMillis =
                remainingCooldownMillis(
                        lastUseMillis,
                        cooldownMillis
                );

        return formatShortTime(
                remainingMillis / 1000L
        );
    }

    public static long secondsToTicks(long seconds) {
        if (seconds <= 0L) return 0L;

        if (
                seconds
                        > Long.MAX_VALUE
                        / TICKS_PER_SECOND
        ) {
            return Long.MAX_VALUE;
        }

        return seconds * TICKS_PER_SECOND;
    }

    public static long minutesToTicks(long minutes) {
        if (minutes <= 0L) return 0L;

        if (
                minutes
                        > Long.MAX_VALUE
                        / TICKS_PER_MINUTE
        ) {
            return Long.MAX_VALUE;
        }

        return minutes * TICKS_PER_MINUTE;
    }

    public static long ticksToSeconds(long ticks) {
        return Math.max(0L, ticks) / TICKS_PER_SECOND;
    }
}