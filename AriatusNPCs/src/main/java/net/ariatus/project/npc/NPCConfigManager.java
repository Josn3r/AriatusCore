package net.ariatus.project.npc;

import net.ariatus.project.AriatusNPCs;
import net.ariatus.project.api.npc.NPCClickType;
import net.ariatus.project.api.npc.NPCEngineType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NPCConfigManager {

    private static final String FILE = "npcs.yml";

    private final AriatusNPCs module;

    public NPCConfigManager(AriatusNPCs module) {
        this.module = module;
    }

    public Map<String, AriatusNPC> loadNPCs() {
        Map<String, AriatusNPC> npcs = new LinkedHashMap<>();

        ConfigurationSection section = module.config(FILE).getConfigurationSection("npcs");

        if (section == null) {
            return npcs;
        }

        for (String id : section.getKeys(false)) {
            try {
                AriatusNPC npc = loadNPC(id);
                npcs.put(npc.id(), npc);
            } catch (Exception exception) {
                module.logger().warn(module, "No se pudo cargar NPC " + id + ": " + exception.getMessage());
            }
        }

        return npcs;
    }

    public AriatusNPC loadNPC(String id) {
        String base = "npcs." + id + ".";

        NPCEngineType engineType = NPCEngineType.valueOf(
                module.configString(FILE, base + "engine", "ENTITY").toUpperCase()
        );

        EntityType entityType = EntityType.valueOf(
                module.configString(FILE, base + "type", "VILLAGER").toUpperCase()
        );

        Location location = location(base + "location");

        AriatusNPC npc = new AriatusNPC(
                id.toLowerCase(),
                engineType,
                entityType,
                location
        );

        npc.enabled(module.configBoolean(FILE, base + "enabled", true));
        npc.displayName(module.configString(FILE, base + "name.text", id));
        npc.nameVisible(module.configBoolean(FILE, base + "name.visible", true));

        npc.ai(module.configBoolean(FILE, base + "properties.ai",
                module.configBoolean("config.yml", "npcs.defaults.ai", false)));

        npc.silent(module.configBoolean(FILE, base + "properties.silent",
                module.configBoolean("config.yml", "npcs.defaults.silent", true)));

        npc.invulnerable(module.configBoolean(FILE, base + "properties.invulnerable",
                module.configBoolean("config.yml", "npcs.defaults.invulnerable", true)));

        npc.gravity(module.configBoolean(FILE, base + "properties.gravity",
                module.configBoolean("config.yml", "npcs.defaults.gravity", true)));

        npc.glowing(module.configBoolean(FILE, base + "properties.glowing",
                module.configBoolean("config.yml", "npcs.defaults.glowing", false)));

        String glowingColor = module.configString(FILE, base + "properties.glowing-color", "DISABLED");

        try {
            npc.glowingColor(NPCGlowingColor.valueOf(glowingColor.toUpperCase()));
            npc.glowing(npc.glowingColor() != NPCGlowingColor.DISABLED);
        } catch (Exception exception) {
            npc.glowingColor(NPCGlowingColor.DISABLED);
            npc.glowing(false);
        }

        npc.collidable(module.configBoolean(FILE, base + "properties.collidable",
                module.configBoolean("config.yml", "npcs.defaults.collidable", false)));

        npc.persistent(module.configBoolean(FILE, base + "properties.persistent",
                module.configBoolean("config.yml", "npcs.defaults.persistent", true)));

        npc.size(module.config("npcs.yml").getDouble(base + "properties.size",
                module.config("config.yml").getDouble("npcs.defaults.size", 1.0)));

        npc.showInTab(module.configBoolean(FILE, base + "player.show-in-tab", false));
        npc.interactionCooldownMillis(module.config("npcs.yml").getLong(base + "interaction.cooldown", 350L));

        try {
            npc.glowingColor(NPCGlowingColor.valueOf(glowingColor.toUpperCase()));
            npc.glowing(npc.glowingColor() != NPCGlowingColor.DISABLED);
        } catch (Exception exception) {
            npc.glowingColor(NPCGlowingColor.DISABLED);
            npc.glowing(false);
        }

        npc.turnToPlayer(module.configBoolean(FILE, base + "look.turn-to-player", false));
        npc.turnToPlayerDistance(module.configDouble(FILE, base + "look.turn-to-player-distance", 8.0));

        loadCustomAttributes(npc, base + "custom-attributes");
        loadVisibility(npc, base + "visibility");

        loadSkin(npc, base + "skin");

        loadActions(npc, base + "actions");

        return npc;
    }

    private Location location(String path) {
        String worldName = module.configString(FILE, path + ".world", "world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            throw new IllegalStateException("Mundo no cargado: " + worldName);
        }

        double x = module.config(FILE).getDouble(path + ".x", 0.0);
        double y = module.config(FILE).getDouble(path + ".y", 80.0);
        double z = module.config(FILE).getDouble(path + ".z", 0.0);
        float yaw = (float) module.config(FILE).getDouble(path + ".yaw", 0.0);
        float pitch = (float) module.config(FILE).getDouble(path + ".pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private void loadActions(AriatusNPC npc, String path) {
        ConfigurationSection section = module.config(FILE).getConfigurationSection(path);

        if (section == null) {
            return;
        }

        for (String clickName : section.getKeys(false)) {
            try {
                NPCClickType clickType = NPCClickType.valueOf(clickName.toUpperCase());
                List<NPCAction> actions = new ArrayList<>();

                for (String raw : section.getStringList(clickName)) {
                    actions.add(NPCAction.parse(raw));
                }

                npc.actions(clickType, actions);
            } catch (Exception exception) {
                module.logger().warn(module, "Acción inválida en NPC " + npc.id() + " click " + clickName);
            }
        }
    }

    public void saveNPC(AriatusNPC npc) throws IOException {
        String base = "npcs." + npc.id() + ".";

        module.config(FILE).set(base + "enabled", npc.enabled());
        module.config(FILE).set(base + "engine", npc.engineType().name());
        module.config(FILE).set(base + "type", npc.entityType().name());
        if (npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            module.config(FILE).set(base + "player.skin-name", npc.id());
            module.config(FILE).set(base + "player.listed-in-tab", false);
        }

        module.config(FILE).set(base + "location.world", npc.location().getWorld().getName());
        module.config(FILE).set(base + "location.x", npc.location().getX());
        module.config(FILE).set(base + "location.y", npc.location().getY());
        module.config(FILE).set(base + "location.z", npc.location().getZ());
        module.config(FILE).set(base + "location.yaw", npc.location().getYaw());
        module.config(FILE).set(base + "location.pitch", npc.location().getPitch());

        module.config(FILE).set(base + "name.visible", npc.nameVisible());
        module.config(FILE).set(base + "name.text", npc.displayName());

        module.config(FILE).set(base + "properties.ai", npc.ai());
        module.config(FILE).set(base + "properties.silent", npc.silent());
        module.config(FILE).set(base + "properties.invulnerable", npc.invulnerable());
        module.config(FILE).set(base + "properties.gravity", npc.gravity());
        module.config(FILE).set(base + "properties.glowing", npc.glowing());
        module.config(FILE).set(base + "properties.collidable", npc.collidable());
        module.config(FILE).set(base + "properties.persistent", npc.persistent());
        module.config(FILE).set(base + "properties.size", npc.size());

        module.config(FILE).set(base + "player.show-in-tab", npc.showInTab());

        module.config(FILE).set(base + "look.turn-to-player", npc.turnToPlayer());
        module.config(FILE).set(base + "look.turn-to-player-distance", npc.turnToPlayerDistance());

        module.config(FILE).set(base + "interaction.cooldown", npc.interactionCooldownMillis());

        module.config(FILE).set(base + "properties.glowing-color", npc.glowingColor().name());
        module.config(FILE).set(base + "properties.collidable", npc.collidable());
        module.config(FILE).set(base + "properties.size", npc.size());

        saveSkin(npc, base + "skin");
        saveCustomAttributes(npc, base + "custom-attributes");
        saveVisibility(npc, base + "visibility");
        saveActions(npc, base + "actions");

        module.saveConfig(FILE);
    }

    public void deleteNPC(String id) throws IOException {
        module.config(FILE).set("npcs." + id.toLowerCase(), null);
        module.saveConfig(FILE);
    }

    private void saveSkin(AriatusNPC npc, String path) {
        module.config(FILE).set(path + ".mode", npc.skinData().mode().name());
        module.config(FILE).set(path + ".source", npc.skinData().source());
        module.config(FILE).set(path + ".slim", npc.skinData().slim());
        module.config(FILE).set(path + ".value", npc.skinData().value());
        module.config(FILE).set(path + ".signature", npc.skinData().signature());
    }

    private void saveActions(AriatusNPC npc, String path) {
        module.config(FILE).set(path, null);

        for (var entry : npc.actions().entrySet()) {
            java.util.List<String> serialized = entry.getValue()
                    .stream()
                    .map(action -> {
                        if (action.value() == null || action.value().isBlank()) {
                            return action.type().name();
                        }

                        return action.type().name() + ":" + action.value();
                    })
                    .toList();

            module.config(FILE).set(path + "." + entry.getKey().name(), serialized);
        }
    }

    private void loadSkin(AriatusNPC npc, String path) {
        try {
            String mode = module.configString(FILE, path + ".mode", "NONE");
            String source = module.configString(FILE, path + ".source", "");
            boolean slim = module.configBoolean(FILE, path + ".slim", false);
            String value = module.configString(FILE, path + ".value", "");
            String signature = module.configString(FILE, path + ".signature", "");

            npc.skinData().mode(net.ariatus.project.npc.skin.NPCSkinMode.valueOf(mode.toUpperCase()));
            npc.skinData().source(source);
            npc.skinData().slim(slim);
            npc.skinData().value(value);
            npc.skinData().signature(signature);
        } catch (Exception exception) {
            npc.skinData().mode(net.ariatus.project.npc.skin.NPCSkinMode.NONE);
            npc.skinData().source("");
        }
    }

    private void loadCustomAttributes(AriatusNPC npc, String path) {
        npc.invisible(module.configBoolean(FILE, path + ".invisible", false));
        npc.onFire(module.configBoolean(FILE, path + ".on-fire", false));
        npc.shaking(module.configBoolean(FILE, path + ".shaking", false));

        String rawPose = module.configString(FILE, path + ".pose", "STANDING");

        try {
            npc.pose(net.ariatus.project.npc.attribute.NPCPoseType.parse(rawPose));
        } catch (Exception exception) {
            npc.pose(net.ariatus.project.npc.attribute.NPCPoseType.STANDING);
        }
    }

    private void loadVisibility(AriatusNPC npc, String path) {
        String rawMode = module.configString(FILE, path + ".mode", "ALL");

        try {
            npc.visibilityMode(net.ariatus.project.npc.visibility.NPCVisibilityMode.parse(rawMode));
        } catch (Exception exception) {
            npc.visibilityMode(net.ariatus.project.npc.visibility.NPCVisibilityMode.ALL);
        }

        String rawDistanceMode = module.configString(FILE, path + ".distance.mode", "DEFAULT");

        try {
            npc.visibilityDistanceMode(
                    net.ariatus.project.npc.visibility.NPCVisibilityDistanceMode.valueOf(rawDistanceMode.toUpperCase())
            );
        } catch (Exception exception) {
            npc.visibilityDistanceMode(net.ariatus.project.npc.visibility.NPCVisibilityDistanceMode.DEFAULT);
        }

        npc.visibilityDistance(module.config(FILE).getDouble(path + ".distance.value", -1.0));
    }

    private void saveCustomAttributes(AriatusNPC npc, String path) {
        module.config(FILE).set(path + ".invisible", npc.invisible());
        module.config(FILE).set(path + ".on-fire", npc.onFire());
        module.config(FILE).set(path + ".shaking", npc.shaking());
        module.config(FILE).set(path + ".pose", npc.pose().name());
    }

    private void saveVisibility(AriatusNPC npc, String path) {
        module.config(FILE).set(path + ".mode", npc.visibilityMode().name());
        module.config(FILE).set(path + ".distance.mode", npc.visibilityDistanceMode().name());
        module.config(FILE).set(path + ".distance.value", npc.visibilityDistance());
    }

}