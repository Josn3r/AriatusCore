package net.ariatus.project.utils;

import net.ariatus.project.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AriatusUtil {

    private static final DecimalFormat TWO_DECIMALS = new DecimalFormat("#.##");

    private AriatusUtil() {
    }

    /*
     * -------------------------------------------------------------------------
     * Messages
     * -------------------------------------------------------------------------
     */

    public static Component text(String message) {
        return MessageService.parse(message);
    }

    public static void message(CommandSender sender, String message) {
        MessageService.send(sender, message);
    }

    public static void message(Player player, String message) {
        MessageService.send(player, message);
    }

    public static void blank(CommandSender sender) {
        MessageService.send(sender, "");
    }

    public static void header(CommandSender sender, String title) {
        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#FACC15:#F97316><bold>" + title + "</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
    }

    public static void footer(CommandSender sender) {
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    /*
     * -------------------------------------------------------------------------
     * Text
     * -------------------------------------------------------------------------
     */

    public static String smallCaps(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (char character : input.toCharArray()) {
            builder.append(toSmallCaps(character));
        }

        return builder.toString();
    }

    public static String capitalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = input.trim().toLowerCase(Locale.ROOT);

        return normalized.substring(0, 1).toUpperCase(Locale.ROOT)
                + normalized.substring(1);
    }

    public static String titleCase(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String[] parts = input.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }

            builder.append(capitalize(part));
        }

        return builder.toString();
    }

    public static String cleanId(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");
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

    /*
     * -------------------------------------------------------------------------
     * Time
     * -------------------------------------------------------------------------
     */

    public static String formatHms(long totalSeconds) {
        long safeSeconds = Math.max(0, totalSeconds);

        long hours = safeSeconds / 3600;
        long minutes = (safeSeconds % 3600) / 60;
        long seconds = safeSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String formatShortTime(long totalSeconds) {
        long safeSeconds = Math.max(0, totalSeconds);

        long days = safeSeconds / 86400;
        long hours = (safeSeconds % 86400) / 3600;
        long minutes = (safeSeconds % 3600) / 60;
        long seconds = safeSeconds % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }

        return seconds + "s";
    }

    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "00:00:00";
        }

        return formatHms(duration.toSeconds());
    }

    public static boolean hasCooldown(long lastUseMillis, long cooldownMillis) {
        return remainingCooldownMillis(lastUseMillis, cooldownMillis) > 0;
    }

    public static long remainingCooldownMillis(long lastUseMillis, long cooldownMillis) {
        if (lastUseMillis <= 0 || cooldownMillis <= 0) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - lastUseMillis;
        return Math.max(0, cooldownMillis - elapsed);
    }

    public static String formatCooldown(long lastUseMillis, long cooldownMillis) {
        long remainingMillis = remainingCooldownMillis(lastUseMillis, cooldownMillis);
        return formatShortTime(remainingMillis / 1000);
    }

    /*
     * -------------------------------------------------------------------------
     * Numbers / Math
     * -------------------------------------------------------------------------
     */

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
        if (decimals <= 0) {
            return Math.round(value);
        }

        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    public static String formatDecimal(double value) {
        return TWO_DECIMALS.format(value);
    }

    public static double percent(double current, double max) {
        if (max <= 0) {
            return 0.0;
        }

        return clamp(current / max, 0.0, 1.0);
    }

    public static int percentInt(double current, double max) {
        return (int) Math.round(percent(current, max) * 100.0);
    }

    public static boolean chance(double chance) {
        double safeChance = clamp(chance, 0.0, 1.0);
        return Math.random() <= safeChance;
    }

    public static double distanceSquared(Location first, Location second) {
        if (first == null || second == null) {
            return Double.MAX_VALUE;
        }

        if (first.getWorld() == null || second.getWorld() == null) {
            return Double.MAX_VALUE;
        }

        if (!Objects.equals(first.getWorld().getName(), second.getWorld().getName())) {
            return Double.MAX_VALUE;
        }

        return first.distanceSquared(second);
    }

    public static boolean inRange(Location first, Location second, double range) {
        if (range <= 0) {
            return false;
        }

        return distanceSquared(first, second) <= range * range;
    }

    /*
     * -------------------------------------------------------------------------
     * Players
     * -------------------------------------------------------------------------
     */

    public static Optional<Player> player(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(Bukkit.getPlayerExact(name));
    }

    public static Optional<Player> player(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

    public static boolean online(UUID uuid) {
        return player(uuid)
                .map(Player::isOnline)
                .orElse(false);
    }

    /*
     * -------------------------------------------------------------------------
     * Teleport
     * -------------------------------------------------------------------------
     */

    public static boolean safeTeleport(Player player, Location location) {
        if (player == null || location == null) {
            return false;
        }

        if (!player.isOnline()) {
            return false;
        }

        if (location.getWorld() == null) {
            return false;
        }

        if (!Bukkit.isPrimaryThread()) {
            return false;
        }

        return player.teleport(location);
    }

    public static void safeTeleport(Plugin plugin, Player player, Location location) {
        if (plugin == null || player == null || location == null) {
            return;
        }

        if (!player.isOnline()) {
            return;
        }

        if (location.getWorld() == null) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            player.teleport(location);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            player.teleport(location);
        });
    }

    public static Optional<Location> location(
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        if (worldName == null || worldName.isBlank()) {
            return Optional.empty();
        }

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            return Optional.empty();
        }

        return Optional.of(new Location(world, x, y, z, yaw, pitch));
    }

    /*
     * -------------------------------------------------------------------------
     * Sounds
     * -------------------------------------------------------------------------
     */

    public static void playSound(Player player, Sound sound) {
        playSound(player, sound, 1.0f, 1.0f);
    }

    public static void playSound(Player player, String sound) {
        playSound(player, sound, 1.0f, 1.0f);
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null || !player.isOnline()) {
            return;
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void playSound(Player player, String sound, float volume, float pitch) {
        if (player == null || sound == null || !player.isOnline()) {
            return;
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void playSound(
            Player player,
            Location location,
            Sound sound,
            float volume,
            float pitch
    ) {
        if (player == null || location == null || sound == null || !player.isOnline()) {
            return;
        }

        player.playSound(location, sound, volume, pitch);
    }

    public static void playSound(
            Player player,
            Location location,
            String sound,
            float volume,
            float pitch
    ) {
        if (player == null || location == null || sound == null || !player.isOnline()) {
            return;
        }

        player.playSound(location, sound, volume, pitch);
    }

    public static void playSound(
            Player player,
            Sound sound,
            SoundCategory category,
            float volume,
            float pitch
    ) {
        if (player == null || sound == null || category == null || !player.isOnline()) {
            return;
        }

        player.playSound(player.getLocation(), sound, category, volume, pitch);
    }

    public static void playSound(
            Player player,
            String sound,
            SoundCategory category,
            float volume,
            float pitch
    ) {
        if (player == null || sound == null || category == null || !player.isOnline()) {
            return;
        }

        player.playSound(player.getLocation(), sound, category, volume, pitch);
    }

    public static void stopSound(Player player, Sound sound) {
        if (player == null || sound == null || !player.isOnline()) {
            return;
        }
        player.stopSound(sound);
    }

    public static void stopSound(Player player, String sound) {
        if (player == null || sound == null || !player.isOnline()) {
            return;
        }
        player.stopSound(sound);
    }

    public static void stopSound(Player player, Sound sound, SoundCategory category) {
        if (player == null || sound == null || category == null || !player.isOnline()) {
            return;
        }
        player.stopSound(sound, category);
    }
    
    public static void stopSound(Player player, String sound, SoundCategory category) {
        if (player == null || sound == null || category == null || !player.isOnline()) {
            return;
        }
        player.stopSound(sound, category);
    }

    public static void stopAllSounds(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.stopAllSounds();
    }

    /*
     * -------------------------------------------------------------------------
     * Progress / Bars
     * -------------------------------------------------------------------------
     */

    public static String progressBar(
            double current,
            double max,
            int bars,
            String filled,
            String empty
    ) {
        int safeBars = Math.max(1, bars);
        double progress = percent(current, max);

        int filledBars = clamp((int) Math.round(progress * safeBars), 0, safeBars);
        int emptyBars = safeBars - filledBars;

        return filled.repeat(filledBars) + empty.repeat(emptyBars);
    }

    public static String progressBar(double current, double max) {
        return progressBar(current, max, 10, "■", "□");
    }

    /*
     * -------------------------------------------------------------------------
     * Roman numerals
     * -------------------------------------------------------------------------
     */

    public static String roman(int number) {
        if (number <= 0) {
            return String.valueOf(number);
        }

        int[] values = {
                1000, 900, 500, 400,
                100, 90, 50, 40,
                10, 9, 5, 4,
                1
        };

        String[] symbols = {
                "M", "CM", "D", "CD",
                "C", "XC", "L", "XL",
                "X", "IX", "V", "IV",
                "I"
        };

        StringBuilder builder = new StringBuilder();
        int remaining = number;

        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                builder.append(symbols[i]);
                remaining -= values[i];
            }
        }

        return builder.toString();
    }
}