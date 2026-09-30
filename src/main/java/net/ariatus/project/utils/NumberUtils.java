package net.ariatus.project.utils;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

public final class NumberUtils {

    private static final ThreadLocal<DecimalFormat> TWO_DECIMALS = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat("#.##");
        format.setGroupingUsed(false);
        return format;
    });

    private NumberUtils() {
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double round(double value, int decimals) {
        if (decimals <= 0) return Math.round(value);

        double factor = Math.pow(10.0D, decimals);
        return Math.round(value * factor) / factor;
    }

    public static String formatDecimal(double value) {
        return TWO_DECIMALS.get().format(value);
    }

    public static double percent(double current, double max) {
        if (max <= 0.0D) return 0.0D;
        return clamp(current / max, 0.0D, 1.0D);
    }

    public static int percentInt(double current, double max) {
        return (int) Math.round(percent(current, max) * 100.0D);
    }

    public static boolean chance(double chance) {
        double safeChance = clamp(chance, 0.0D, 1.0D);

        if (safeChance <= 0.0D) return false;
        if (safeChance >= 1.0D) return true;

        return ThreadLocalRandom.current().nextDouble() < safeChance;
    }
}