package net.ariatus.project.utils;

import net.ariatus.project.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Deprecated(
        since = "2.0.0",
        forRemoval = false
)
public final class AriatusUtil {

    private AriatusUtil() {
    }

    public static Component text(
            String message
    ) {
        return MessageService.parse(
                message
        );
    }

    public static void message(
            CommandSender sender,
            String message
    ) {
        MessageService.send(
                sender,
                message
        );
    }

    public static void message(
            Player player,
            String message
    ) {
        MessageService.send(
                player,
                message
        );
    }

    public static void blank(
            CommandSender sender
    ) {
        MessageService.blank(
                sender
        );
    }

    public static void header(
            CommandSender sender,
            String title
    ) {
        MessageService.header(
                sender,
                title
        );
    }

    public static void footer(
            CommandSender sender
    ) {
        MessageService.footer(
                sender
        );
    }

    public static String smallCaps(
            String input
    ) {
        return TextUtils.smallCaps(
                input
        );
    }

    public static String capitalize(
            String input
    ) {
        return TextUtils.capitalize(
                input
        );
    }

    public static String titleCase(
            String input
    ) {
        return TextUtils.titleCase(
                input
        );
    }

    public static String cleanId(
            String input
    ) {
        return TextUtils.cleanId(
                input
        );
    }

    public static String formatHms(
            long totalSeconds
    ) {
        return TimeUtils.formatHms(
                totalSeconds
        );
    }

    public static String formatShortTime(
            long totalSeconds
    ) {
        return TimeUtils.formatShortTime(
                totalSeconds
        );
    }

    public static String formatDuration(
            Duration duration
    ) {
        return TimeUtils.formatDuration(
                duration
        );
    }

    public static boolean hasCooldown(
            long lastUseMillis,
            long cooldownMillis
    ) {
        return TimeUtils.hasCooldown(
                lastUseMillis,
                cooldownMillis
        );
    }

    public static long remainingCooldownMillis(
            long lastUseMillis,
            long cooldownMillis
    ) {
        return TimeUtils.remainingCooldownMillis(
                lastUseMillis,
                cooldownMillis
        );
    }

    public static String formatCooldown(
            long lastUseMillis,
            long cooldownMillis
    ) {
        return TimeUtils.formatCooldown(
                lastUseMillis,
                cooldownMillis
        );
    }

    public static int clamp(
            int value,
            int min,
            int max
    ) {
        return NumberUtils.clamp(
                value,
                min,
                max
        );
    }

    public static long clamp(
            long value,
            long min,
            long max
    ) {
        return NumberUtils.clamp(
                value,
                min,
                max
        );
    }

    public static double clamp(
            double value,
            double min,
            double max
    ) {
        return NumberUtils.clamp(
                value,
                min,
                max
        );
    }

    public static double round(
            double value,
            int decimals
    ) {
        return NumberUtils.round(
                value,
                decimals
        );
    }

    public static String formatDecimal(
            double value
    ) {
        return NumberUtils.formatDecimal(
                value
        );
    }

    public static double percent(
            double current,
            double max
    ) {
        return NumberUtils.percent(
                current,
                max
        );
    }

    public static int percentInt(
            double current,
            double max
    ) {
        return NumberUtils.percentInt(
                current,
                max
        );
    }

    public static boolean chance(
            double chance
    ) {
        return NumberUtils.chance(
                chance
        );
    }

    public static double distanceSquared(
            Location first,
            Location second
    ) {
        return LocationUtils.distanceSquared(
                first,
                second
        );
    }

    public static boolean inRange(
            Location first,
            Location second,
            double range
    ) {
        return LocationUtils.inRange(
                first,
                second,
                range
        );
    }

    public static Optional<Player> player(
            String name
    ) {
        return PlayerUtils.player(
                name
        );
    }

    public static Optional<Player> player(
            UUID uuid
    ) {
        return PlayerUtils.player(
                uuid
        );
    }

    public static boolean online(
            UUID uuid
    ) {
        return PlayerUtils.online(
                uuid
        );
    }

    public static boolean safeTeleport(
            Player player,
            Location location
    ) {
        return PlayerUtils.safeTeleport(
                player,
                location
        );
    }

    public static void safeTeleport(
            Plugin plugin,
            Player player,
            Location location
    ) {
        PlayerUtils.safeTeleport(
                plugin,
                player,
                location
        );
    }

    public static Optional<Location> location(
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        return LocationUtils.location(
                worldName,
                x,
                y,
                z,
                yaw,
                pitch
        );
    }

    public static void playSound(
            Player player,
            Sound sound
    ) {
        SoundUtils.play(
                player,
                sound
        );
    }

    public static void playSound(
            Player player,
            String sound
    ) {
        SoundUtils.play(
                player,
                sound
        );
    }

    public static void playSound(
            Player player,
            Sound sound,
            float volume,
            float pitch
    ) {
        SoundUtils.play(
                player,
                sound,
                volume,
                pitch
        );
    }

    public static void playSound(
            Player player,
            String sound,
            float volume,
            float pitch
    ) {
        SoundUtils.play(
                player,
                sound,
                volume,
                pitch
        );
    }

    public static void playSound(
            Player player,
            Location location,
            Sound sound,
            float volume,
            float pitch
    ) {
        SoundUtils.play(
                player,
                location,
                sound,
                volume,
                pitch
        );
    }

    public static void playSound(
            Player player,
            Location location,
            String sound,
            float volume,
            float pitch
    ) {
        SoundUtils.play(
                player,
                location,
                sound,
                volume,
                pitch
        );
    }

    public static void playSound(
            Player player,
            Sound sound,
            SoundCategory category,
            float volume,
            float pitch
    ) {
        SoundUtils.play(
                player,
                sound,
                category,
                volume,
                pitch
        );
    }

    public static void playSound(
            Player player,
            String sound,
            SoundCategory category,
            float volume,
            float pitch
    ) {
        SoundUtils.play(
                player,
                sound,
                category,
                volume,
                pitch
        );
    }

    public static void stopSound(
            Player player,
            Sound sound
    ) {
        SoundUtils.stop(
                player,
                sound
        );
    }

    public static void stopSound(
            Player player,
            String sound
    ) {
        SoundUtils.stop(
                player,
                sound
        );
    }

    public static void stopSound(
            Player player,
            Sound sound,
            SoundCategory category
    ) {
        SoundUtils.stop(
                player,
                sound,
                category
        );
    }

    public static void stopSound(
            Player player,
            String sound,
            SoundCategory category
    ) {
        SoundUtils.stop(
                player,
                sound,
                category
        );
    }

    public static void stopAllSounds(
            Player player
    ) {
        SoundUtils.stopAll(
                player
        );
    }

    public static String progressBar(
            double current,
            double max,
            int bars,
            String filled,
            String empty
    ) {
        return TextUtils.progressBar(
                current,
                max,
                bars,
                filled,
                empty
        );
    }

    public static String progressBar(
            double current,
            double max
    ) {
        return TextUtils.progressBar(
                current,
                max
        );
    }

    public static String roman(
            int number
    ) {
        return TextUtils.roman(
                number
        );
    }
}