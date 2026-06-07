package net.ariatus.project.world;

import net.ariatus.project.AriatusWorlds;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class WorldConfigManager {

    private static final String WORLDS_FILE = "worlds.yml";

    private final AriatusWorlds module;

    public WorldConfigManager(AriatusWorlds module) {
        this.module = module;
    }

    public Map<String, AriatusWorld> loadWorlds() {
        Map<String, AriatusWorld> worlds = new LinkedHashMap<>();

        ConfigurationSection section = module.config(WORLDS_FILE).getConfigurationSection("worlds");

        if (section == null) {
            module.logger().warn(module, "No hay mundos configurados en worlds.yml.");
            return worlds;
        }

        for (String id : section.getKeys(false)) {
            String path = "worlds." + id + ".";

            AriatusWorld world = new AriatusWorld(
                    id.toLowerCase(),
                    section.getString(id + ".display-name", id),
                    section.getString(id + ".folder", id),
                    section.getString(id + ".environment", "NORMAL"),
                    section.getString(id + ".type", "NORMAL"),
                    section.getBoolean(id + ".generate-structures", false),
                    section.getString(id + ".seed", ""),
                    section.getBoolean(id + ".enabled", true),
                    section.getBoolean(id + ".auto-load", false),
                    false
            );

            worlds.put(world.id(), world);
        }

        module.logger().info(module, "Mundos configurados cargados: " + worlds.size());
        return worlds;
    }

    public Optional<Location> spawn(String worldId, String spawnName) {
        String path = "worlds." + worldId.toLowerCase() + ".spawn." + spawnName.toLowerCase();

        if (!module.config(WORLDS_FILE).contains(path)) {
            return Optional.empty();
        }

        String worldName = module.configString(WORLDS_FILE, path + ".world", worldId);
        World world = org.bukkit.Bukkit.getWorld(worldName);

        if (world == null) {
            return Optional.empty();
        }

        double x = module.config(WORLDS_FILE).getDouble(path + ".x", 0.5);
        double y = module.config(WORLDS_FILE).getDouble(path + ".y", 80.0);
        double z = module.config(WORLDS_FILE).getDouble(path + ".z", 0.5);
        float yaw = (float) module.config(WORLDS_FILE).getDouble(path + ".yaw", 0.0);
        float pitch = (float) module.config(WORLDS_FILE).getDouble(path + ".pitch", 0.0);

        return Optional.of(new Location(world, x, y, z, yaw, pitch));
    }

    public void setSpawn(String worldId, String spawnName, Location location) {
        String path = "worlds." + worldId.toLowerCase() + ".spawn." + spawnName.toLowerCase();

        module.config(WORLDS_FILE).set(path + ".world", location.getWorld().getName());
        module.config(WORLDS_FILE).set(path + ".x", location.getX());
        module.config(WORLDS_FILE).set(path + ".y", location.getY());
        module.config(WORLDS_FILE).set(path + ".z", location.getZ());
        module.config(WORLDS_FILE).set(path + ".yaw", location.getYaw());
        module.config(WORLDS_FILE).set(path + ".pitch", location.getPitch());

        module.saveConfig(WORLDS_FILE);
    }

    public Optional<WorldBorderData> border(String worldId) {
        String path = "worlds." + worldId.toLowerCase() + ".border";

        if (!module.config(WORLDS_FILE).contains(path)) {
            return Optional.empty();
        }

        boolean enabled = module.configBoolean(WORLDS_FILE, path + ".enabled", false);

        if (!enabled) {
            return Optional.empty();
        }

        return Optional.of(new WorldBorderData(
                true,
                module.configString(WORLDS_FILE, path + ".type", "RECTANGLE"),
                module.config(WORLDS_FILE).getDouble(path + ".min-x", -1000),
                module.config(WORLDS_FILE).getDouble(path + ".max-x", 1000),
                module.config(WORLDS_FILE).getDouble(path + ".min-z", -1000),
                module.config(WORLDS_FILE).getDouble(path + ".max-z", 1000),
                module.configString(WORLDS_FILE, path + ".action", "PUSH_BACK"),
                module.config(WORLDS_FILE).getDouble(path + ".warning-distance", 10),
                module.configString(WORLDS_FILE, path + ".message", "&cHas llegado al límite del mundo."),
                module.configString(WORLDS_FILE, path + ".sound", "BLOCK_ANVIL_LAND")
        ));
    }

    public void setRectangleBorder(
            String worldId,
            double minX,
            double maxX,
            double minZ,
            double maxZ
    ) throws IOException {
        String path = "worlds." + worldId.toLowerCase() + ".border";

        module.config(WORLDS_FILE).set(path + ".enabled", true);
        module.config(WORLDS_FILE).set(path + ".type", "RECTANGLE");
        module.config(WORLDS_FILE).set(path + ".min-x", minX);
        module.config(WORLDS_FILE).set(path + ".max-x", maxX);
        module.config(WORLDS_FILE).set(path + ".min-z", minZ);
        module.config(WORLDS_FILE).set(path + ".max-z", maxZ);
        module.config(WORLDS_FILE).set(path + ".action", "PUSH_BACK");
        module.config(WORLDS_FILE).set(path + ".warning-distance", 10);
        module.config(WORLDS_FILE).set(path + ".message", "&cHas llegado al límite del mundo.");
        module.config(WORLDS_FILE).set(path + ".sound", "BLOCK_ANVIL_LAND");

        module.saveConfig(WORLDS_FILE);
    }
}