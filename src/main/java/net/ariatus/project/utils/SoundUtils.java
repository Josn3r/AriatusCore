package net.ariatus.project.utils;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public final class SoundUtils {

    private SoundUtils() {
    }

    public static void play(
            Player player,
            Sound sound
    ) {
        play(
                player,
                sound,
                1.0F,
                1.0F
        );
    }

    public static void play(
            Player player,
            String sound
    ) {
        play(
                player,
                sound,
                1.0F,
                1.0F
        );
    }

    public static void play(
            Player player,
            Sound sound,
            float volume,
            float pitch
    ) {
        if (!valid(player) || sound == null) return;

        player.playSound(
                player.getLocation(),
                sound,
                volume,
                pitch
        );
    }

    public static void play(
            Player player,
            String sound,
            float volume,
            float pitch
    ) {
        if (
                !valid(player)
                        || sound == null
                        || sound.isBlank()
        ) {
            return;
        }

        player.playSound(
                player.getLocation(),
                sound,
                volume,
                pitch
        );
    }

    public static void play(
            Player player,
            Location location,
            Sound sound,
            float volume,
            float pitch
    ) {
        if (
                !valid(player)
                        || location == null
                        || sound == null
        ) {
            return;
        }

        player.playSound(
                location,
                sound,
                volume,
                pitch
        );
    }

    public static void play(
            Player player,
            Location location,
            String sound,
            float volume,
            float pitch
    ) {
        if (
                !valid(player)
                        || location == null
                        || sound == null
                        || sound.isBlank()
        ) {
            return;
        }

        player.playSound(
                location,
                sound,
                volume,
                pitch
        );
    }

    public static void play(
            Player player,
            Sound sound,
            SoundCategory category,
            float volume,
            float pitch
    ) {
        if (
                !valid(player)
                        || sound == null
                        || category == null
        ) {
            return;
        }

        player.playSound(
                player.getLocation(),
                sound,
                category,
                volume,
                pitch
        );
    }

    public static void play(
            Player player,
            String sound,
            SoundCategory category,
            float volume,
            float pitch
    ) {
        if (
                !valid(player)
                        || sound == null
                        || sound.isBlank()
                        || category == null
        ) {
            return;
        }

        player.playSound(
                player.getLocation(),
                sound,
                category,
                volume,
                pitch
        );
    }

    public static void stop(
            Player player,
            Sound sound
    ) {
        if (!valid(player) || sound == null) return;

        player.stopSound(
                sound
        );
    }

    public static void stop(
            Player player,
            String sound
    ) {
        if (
                !valid(player)
                        || sound == null
                        || sound.isBlank()
        ) {
            return;
        }

        player.stopSound(
                sound
        );
    }

    public static void stop(
            Player player,
            Sound sound,
            SoundCategory category
    ) {
        if (
                !valid(player)
                        || sound == null
                        || category == null
        ) {
            return;
        }

        player.stopSound(
                sound,
                category
        );
    }

    public static void stop(
            Player player,
            String sound,
            SoundCategory category
    ) {
        if (
                !valid(player)
                        || sound == null
                        || sound.isBlank()
                        || category == null
        ) {
            return;
        }

        player.stopSound(
                sound,
                category
        );
    }

    public static void stopAll(
            Player player
    ) {
        if (!valid(player)) return;

        player.stopAllSounds();
    }

    private static boolean valid(
            Player player
    ) {
        return player != null
                && player.isOnline();
    }
}