package net.ariatus.project.protection;

import net.ariatus.project.AriatusProtection;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProtectionManager {

    private final AriatusProtection module;
    private final Map<String, ProtectionSettings> worlds = new HashMap<>();

    public ProtectionManager(AriatusProtection module) {
        this.module = module;
    }

    public void load() {
        worlds.clear();

        ConfigurationSection section = module.config("config.yml")
                .getConfigurationSection("protection.worlds");

        if (section == null) {
            module.logger().warn(module, "No hay mundos configurados en AriatusProtection.");
            return;
        }

        for (String worldName : section.getKeys(false)) {
            String path = "protection.worlds." + worldName + ".";

            ProtectionSettings settings = new ProtectionSettings(
                    worldName.toLowerCase(),
                    module.configBoolean("config.yml", path + "enabled", true),
                    module.configBoolean("config.yml", path + "block-breaking", false),
                    module.configBoolean("config.yml", path + "block-placing", false),
                    materialSet(path + "blocked-interactions"),
                    materialSet(path + "blocked-inventory-open"),
                    materialSet(path + "allowed-interactions")
            );

            worlds.put(settings.worldName(), settings);
        }

        module.logger().info(module, "Protecciones cargadas: " + worlds.size());
    }

    public Optional<ProtectionSettings> settings(String worldName) {
        return Optional.ofNullable(worlds.get(worldName.toLowerCase()))
                .filter(ProtectionSettings::enabled);
    }

    public boolean protectedWorld(String worldName) {
        return settings(worldName).isPresent();
    }

    public int loadedWorlds() {
        return worlds.size();
    }

    public String bypassPermission() {
        return module.configString(
                "config.yml",
                "protection.bypass-permission",
                "ariatusprotection.bypass"
        );
    }

    public String message(String path, String fallback) {
        return module.configString("config.yml", "protection.messages." + path, fallback);
    }

    private Set<Material> materialSet(String path) {
        Set<Material> materials = new HashSet<>();

        for (String raw : module.config("config.yml").getStringList(path)) {
            try {
                materials.add(Material.valueOf(raw.toUpperCase()));
            } catch (Exception exception) {
                module.logger().warn(module, "Material inválido en " + path + ": " + raw);
            }
        }

        return materials;
    }
}