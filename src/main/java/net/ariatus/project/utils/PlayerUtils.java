package net.ariatus.project.utils;

import net.ariatus.project.module.AriatusModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerUtils {

    private PlayerUtils() {
    }

    public static Optional<Player> player(
            String name
    ) {
        if (
                name == null
                        || name.isBlank()
        ) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                Bukkit.getPlayerExact(
                        name
                )
        );
    }

    public static Optional<Player> player(
            UUID uuid
    ) {
        if (uuid == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                Bukkit.getPlayer(
                        uuid
                )
        );
    }

    public static boolean online(
            UUID uuid
    ) {
        return player(uuid)
                .map(
                        Player::isOnline
                )
                .orElse(
                        false
                );
    }

    public static boolean safeTeleport(
            Player player,
            Location location
    ) {
        if (!validTeleport(player, location)) {
            return false;
        }

        if (!Bukkit.isPrimaryThread()) {
            return false;
        }

        return player.teleport(
                location
        );
    }

    public static void safeTeleport(
            Plugin plugin,
            Player player,
            Location location
    ) {
        Objects.requireNonNull(
                plugin,
                "plugin"
        );

        if (!validTeleport(player, location)) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            player.teleport(
                    location
            );

            return;
        }

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            if (
                                    validTeleport(
                                            player,
                                            location
                                    )
                            ) {
                                player.teleport(
                                        location
                                );
                            }
                        }
                );
    }

    public static void safeTeleport(
            AriatusModule module,
            Player player,
            Location location
    ) {
        Objects.requireNonNull(
                module,
                "module"
        );

        if (!validTeleport(player, location)) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            player.teleport(
                    location
            );

            return;
        }

        module.tasks()
                .run(
                        () -> {
                            if (
                                    validTeleport(
                                            player,
                                            location
                                    )
                            ) {
                                player.teleport(
                                        location
                                );
                            }
                        }
                );
    }

    private static boolean validTeleport(
            Player player,
            Location location
    ) {
        return player != null
                && player.isOnline()
                && location != null
                && location.getWorld() != null;
    }
}