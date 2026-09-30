package net.ariatus.project.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public final class LocationUtils {

    private LocationUtils() {
    }

    public static boolean sameWorld(
            Location first,
            Location second
    ) {
        if (
                first == null
                        || second == null
                        || first.getWorld() == null
                        || second.getWorld() == null
        ) {
            return false;
        }

        return first.getWorld()
                .getUID()
                .equals(
                        second.getWorld()
                                .getUID()
                );
    }

    public static double distanceSquared(
            Location first,
            Location second
    ) {
        if (!sameWorld(first, second)) {
            return Double.MAX_VALUE;
        }

        return first.distanceSquared(
                second
        );
    }

    public static boolean inRange(
            Location first,
            Location second,
            double range
    ) {
        if (range <= 0.0D) return false;

        return distanceSquared(
                first,
                second
        ) <= range * range;
    }

    public static Optional<Location> location(
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        if (
                worldName == null
                        || worldName.isBlank()
        ) {
            return Optional.empty();
        }

        World world =
                Bukkit.getWorld(
                        worldName
                );

        if (world == null) {
            return Optional.empty();
        }

        return Optional.of(
                new Location(
                        world,
                        x,
                        y,
                        z,
                        yaw,
                        pitch
                )
        );
    }
}