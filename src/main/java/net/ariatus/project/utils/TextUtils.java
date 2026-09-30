package net.ariatus.project.utils;

import java.util.Locale;
import java.util.Objects;

public final class TextUtils {

    private static final int[] ROMAN_VALUES = {
            1000, 900, 500, 400,
            100, 90, 50, 40,
            10, 9, 5, 4,
            1
    };

    private static final String[] ROMAN_SYMBOLS = {
            "M", "CM", "D", "CD",
            "C", "XC", "L", "XL",
            "X", "IX", "V", "IV",
            "I"
    };

    private TextUtils() {
    }

    public static String smallCaps(String input) {
        if (input == null || input.isBlank()) return "";

        StringBuilder builder = new StringBuilder(input.length());

        for (char character : input.toCharArray()) {
            builder.append(toSmallCaps(character));
        }

        return builder.toString();
    }

    public static String capitalize(String input) {
        if (input == null || input.isBlank()) return "";

        String normalized = input.trim().toLowerCase(Locale.ROOT);

        return normalized.substring(0, 1).toUpperCase(Locale.ROOT)
                + normalized.substring(1);
    }

    public static String titleCase(String input) {
        if (input == null || input.isBlank()) return "";

        String[] parts = input.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(capitalize(part));
        }

        return builder.toString();
    }

    public static String cleanId(String input) {
        if (input == null || input.isBlank()) return "";

        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");
    }

    public static String progressBar(
            double current,
            double max,
            int bars,
            String filled,
            String empty
    ) {
        int safeBars = Math.max(1, bars);

        String filledSymbol = Objects.requireNonNullElse(filled, "");
        String emptySymbol = Objects.requireNonNullElse(empty, "");

        double progress = NumberUtils.percent(current, max);

        int filledBars = NumberUtils.clamp(
                (int) Math.round(progress * safeBars),
                0,
                safeBars
        );

        int emptyBars = safeBars - filledBars;

        return filledSymbol.repeat(filledBars)
                + emptySymbol.repeat(emptyBars);
    }

    public static String progressBar(double current, double max) {
        return progressBar(
                current,
                max,
                10,
                "■",
                "□"
        );
    }

    public static String roman(int number) {
        if (number <= 0) return String.valueOf(number);

        StringBuilder builder = new StringBuilder();
        int remaining = number;

        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (remaining >= ROMAN_VALUES[i]) {
                builder.append(ROMAN_SYMBOLS[i]);
                remaining -= ROMAN_VALUES[i];
            }
        }

        return builder.toString();
    }

    private static char toSmallCaps(char character) {
        return switch (Character.toLowerCase(character)) {
            case 'a' -> 'ᴀ';
            case 'b' -> 'ʙ';
            case 'c' -> 'ᴄ';
            case 'd' -> 'ᴅ';
            case 'e' -> 'ᴇ';
            case 'f' -> 'ꜰ';
            case 'g' -> 'ɢ';
            case 'h' -> 'ʜ';
            case 'i' -> 'ɪ';
            case 'j' -> 'ᴊ';
            case 'k' -> 'ᴋ';
            case 'l' -> 'ʟ';
            case 'm' -> 'ᴍ';
            case 'n' -> 'ɴ';
            case 'o' -> 'ᴏ';
            case 'p' -> 'ᴘ';
            case 'q' -> 'ǫ';
            case 'r' -> 'ʀ';
            case 's' -> 'ꜱ';
            case 't' -> 'ᴛ';
            case 'u' -> 'ᴜ';
            case 'v' -> 'ᴠ';
            case 'w' -> 'ᴡ';
            case 'x' -> 'x';
            case 'y' -> 'ʏ';
            case 'z' -> 'ᴢ';
            default -> character;
        };
    }
}