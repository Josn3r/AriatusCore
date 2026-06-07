package net.ariatus.project.world;

import org.bukkit.Location;

public record WorldBorderData(
        boolean enabled,
        String type,
        double minX,
        double maxX,
        double minZ,
        double maxZ,
        String action,
        double warningDistance,
        String message,
        String sound
) {

    public boolean inside(Location location) {
        double x = location.getX();
        double z = location.getZ();

        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean near(Location location) {
        double x = location.getX();
        double z = location.getZ();

        return x <= minX + warningDistance
                || x >= maxX - warningDistance
                || z <= minZ + warningDistance
                || z >= maxZ - warningDistance;
    }

    public Location clamp(Location location) {
        Location cloned = location.clone();

        double x = Math.max(minX + 0.5, Math.min(maxX - 0.5, cloned.getX()));
        double z = Math.max(minZ + 0.5, Math.min(maxZ - 0.5, cloned.getZ()));

        cloned.setX(x);
        cloned.setZ(z);

        return cloned;
    }
}