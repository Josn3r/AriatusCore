package net.ariatus.project.world;

import net.ariatus.project.AriatusWorlds;
import net.ariatus.project.api.world.WorldLoadResult;
import net.ariatus.project.api.world.WorldService;
import net.ariatus.project.api.world.WorldTeleportResult;
import net.ariatus.project.api.world.WorldView;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class WorldManager implements WorldService {

    private final AriatusWorlds module;
    private final WorldConfigManager configManager;
    private final Map<String, AriatusWorld> worlds;

    public WorldManager(AriatusWorlds module, WorldConfigManager configManager) {
        this.module = module;
        this.configManager = configManager;
        this.worlds = configManager.loadWorlds();
    }

    public void autoLoadWorlds() {
        if (!module.configBoolean("config.yml", "worlds.auto-load-enabled-worlds", true)) {
            return;
        }

        for (AriatusWorld world : worlds.values()) {
            if (world.enabled() && world.autoLoad()) {
                load(world.id());
            }
        }
    }

    @Override
    public Collection<WorldView> worlds() {
        return java.util.Collections.unmodifiableCollection(worlds.values());
    }

    @Override
    public Optional<WorldView> world(String id) {
        return Optional.ofNullable(worlds.get(id.toLowerCase()));
    }

    public Optional<AriatusWorld> internalWorld(String id) {
        return Optional.ofNullable(worlds.get(id.toLowerCase()));
    }

    @Override
    public boolean exists(String id) {
        return worlds.containsKey(id.toLowerCase());
    }

    @Override
    public boolean loaded(String id) {
        World bukkitWorld = Bukkit.getWorld(folderName(id));

        return bukkitWorld != null;
    }

    @Override
    public CompletableFuture<WorldLoadResult> load(String id) {
        AriatusWorld ariatusWorld = worlds.get(id.toLowerCase());

        if (ariatusWorld == null) {
            return CompletableFuture.completedFuture(WorldLoadResult.NOT_CONFIGURED);
        }

        if (!ariatusWorld.enabled()) {
            return CompletableFuture.completedFuture(WorldLoadResult.DISABLED);
        }

        if (Bukkit.getWorld(ariatusWorld.folder()) != null) {
            ariatusWorld.loaded(true);
            return CompletableFuture.completedFuture(WorldLoadResult.ALREADY_LOADED);
        }

        try {
            WorldCreator creator = new WorldCreator(ariatusWorld.folder());

            creator.environment(parseEnvironment(ariatusWorld.environment()));
            creator.type(parseWorldType(ariatusWorld.type()));
            creator.generateStructures(ariatusWorld.generateStructures());

            if (ariatusWorld.seed() != null && !ariatusWorld.seed().isBlank()) {
                creator.seed(Long.parseLong(ariatusWorld.seed()));
            }

            World created = creator.createWorld();

            if (created == null) {
                return CompletableFuture.completedFuture(WorldLoadResult.FAILED);
            }

            applyRules(ariatusWorld, created);

            ariatusWorld.loaded(true);

            module.logger().info(module, "Mundo cargado: " + ariatusWorld.id() + " (" + ariatusWorld.folder() + ")");
            return CompletableFuture.completedFuture(WorldLoadResult.SUCCESS);

        } catch (Exception exception) {
            module.logger().error(module, "Error cargando mundo " + id + ": " + exception.getMessage());
            return CompletableFuture.completedFuture(WorldLoadResult.FAILED);
        }
    }

    @Override
    public CompletableFuture<Boolean> unload(String id, boolean save) {
        AriatusWorld ariatusWorld = worlds.get(id.toLowerCase());

        if (ariatusWorld == null) {
            return CompletableFuture.completedFuture(false);
        }

        World world = Bukkit.getWorld(ariatusWorld.folder());

        if (world == null) {
            ariatusWorld.loaded(false);
            return CompletableFuture.completedFuture(true);
        }

        boolean preventIfPlayers = module.configBoolean(
                "config.yml",
                "worlds.safety.prevent-unload-if-players-inside",
                true
        );

        if (preventIfPlayers && !world.getPlayers().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        boolean unloaded = Bukkit.unloadWorld(world, save);
        ariatusWorld.loaded(!unloaded);

        return CompletableFuture.completedFuture(unloaded);
    }

    @Override
    public CompletableFuture<Boolean> delete(String id) {
        AriatusWorld ariatusWorld = worlds.get(id.toLowerCase());

        if (ariatusWorld == null) {
            return CompletableFuture.completedFuture(false);
        }

        World world = Bukkit.getWorld(ariatusWorld.folder());

        if (world != null && !world.getPlayers().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        File folder = new File(Bukkit.getWorldContainer(), ariatusWorld.folder());

        if (!folder.exists()) {
            return CompletableFuture.completedFuture(true);
        }

        boolean deleted = deleteFolder(folder);

        if (deleted) {
            ariatusWorld.loaded(false);
        }

        return CompletableFuture.completedFuture(deleted);
    }

    @Override
    public CompletableFuture<WorldTeleportResult> teleport(Player player, String id) {
        return teleport(player, id, "default");
    }

    @Override
    public CompletableFuture<WorldTeleportResult> teleport(Player player, String id, String spawnName) {
        if (player == null || !player.isOnline()) {
            return CompletableFuture.completedFuture(WorldTeleportResult.PLAYER_OFFLINE);
        }

        AriatusWorld ariatusWorld = worlds.get(id.toLowerCase());

        if (ariatusWorld == null) {
            return CompletableFuture.completedFuture(WorldTeleportResult.WORLD_NOT_FOUND);
        }

        return load(id).thenApply(result -> {
            if (result == WorldLoadResult.FAILED || result == WorldLoadResult.DISABLED || result == WorldLoadResult.NOT_CONFIGURED) {
                return WorldTeleportResult.WORLD_NOT_LOADED;
            }

            Optional<Location> spawn = spawn(id, spawnName);

            if (spawn.isEmpty()) {
                return WorldTeleportResult.SPAWN_NOT_SET;
            }

            boolean success = player.teleport(spawn.get());
            return success ? WorldTeleportResult.SUCCESS : WorldTeleportResult.FAILED;
        });
    }

    @Override
    public Optional<Location> spawn(String id) {
        return spawn(id, "default");
    }

    @Override
    public Optional<Location> spawn(String id, String spawnName) {
        return configManager.spawn(id, spawnName);
    }

    public void setSpawn(String id, String spawnName, Location location) {
        configManager.setSpawn(id, spawnName, location);
    }

    private void applyRules(AriatusWorld ariatusWorld, World world) {
        String base = "worlds." + ariatusWorld.id() + ".rules.";

        world.setPVP(module.configBoolean("worlds.yml", base + "pvp", true));
        world.setSpawnFlags(
                module.configBoolean("worlds.yml", base + "monsters", true),
                module.configBoolean("worlds.yml", base + "animals", true)
        );

        String difficultyName = module.configString("worlds.yml", base + "difficulty", "NORMAL");

        try {
            world.setDifficulty(Difficulty.valueOf(difficultyName.toUpperCase()));
        } catch (Exception ignored) {
            world.setDifficulty(Difficulty.NORMAL);
        }

        org.bukkit.configuration.ConfigurationSection gameRules =
                module.config("worlds.yml").getConfigurationSection(base + "game-rule");

        if (gameRules == null) {
            return;
        }

        for (String key : gameRules.getKeys(false)) {
            applyGameRule(world, key, gameRules.getString(key));
        }
    }

    private void applyGameRule(World world, String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }

        try {
            switch (key) {
                case "doMobSpawning" -> setBooleanGameRule(world, GameRule.DO_MOB_SPAWNING, value);
                case "doDaylightCycle" -> setBooleanGameRule(world, GameRule.DO_DAYLIGHT_CYCLE, value);
                case "doWeatherCycle" -> setBooleanGameRule(world, GameRule.DO_WEATHER_CYCLE, value);
                case "keepInventory" -> setBooleanGameRule(world, GameRule.KEEP_INVENTORY, value);
                case "doFireTick" -> setBooleanGameRule(world, GameRule.DO_FIRE_TICK, value);
                case "mobGriefing" -> setBooleanGameRule(world, GameRule.MOB_GRIEFING, value);
                case "doImmediateRespawn" -> setBooleanGameRule(world, GameRule.DO_IMMEDIATE_RESPAWN, value);
                case "showDeathMessages" -> setBooleanGameRule(world, GameRule.SHOW_DEATH_MESSAGES, value);
                case "naturalRegeneration" -> setBooleanGameRule(world, GameRule.NATURAL_REGENERATION, value);
                case "doInsomnia" -> setBooleanGameRule(world, GameRule.DO_INSOMNIA, value);
                case "disableRaids" -> setBooleanGameRule(world, GameRule.DISABLE_RAIDS, value);
                case "announceAdvancements" -> setBooleanGameRule(world, GameRule.ANNOUNCE_ADVANCEMENTS, value);

                case "randomTickSpeed" -> setIntegerGameRule(world, GameRule.RANDOM_TICK_SPEED, value);
                case "spawnRadius" -> setIntegerGameRule(world, GameRule.SPAWN_RADIUS, value);
                case "maxEntityCramming" -> setIntegerGameRule(world, GameRule.MAX_ENTITY_CRAMMING, value);
                case "playersSleepingPercentage" -> setIntegerGameRule(world, GameRule.PLAYERS_SLEEPING_PERCENTAGE, value);

                default -> module.logger().warn(
                        module,
                        "GameRule no soportada por AriatusWorlds: " + key + " en " + world.getName()
                );
            }
        } catch (Exception exception) {
            module.logger().warn(
                    module,
                    "No se pudo aplicar gamerule " + key + " en " + world.getName() + ": " + exception.getMessage()
            );
        }
    }

    private void setBooleanGameRule(World world, GameRule<Boolean> gameRule, String value) {
        world.setGameRule(gameRule, Boolean.parseBoolean(value));
    }

    private void setIntegerGameRule(World world, GameRule<Integer> gameRule, String value) {
        world.setGameRule(gameRule, Integer.parseInt(value));
    }

    private World.Environment parseEnvironment(String environment) {
        try {
            return World.Environment.valueOf(environment.toUpperCase());
        } catch (Exception exception) {
            return World.Environment.NORMAL;
        }
    }

    private WorldType parseWorldType(String type) {
        try {
            return WorldType.valueOf(type.toUpperCase());
        } catch (Exception exception) {
            return WorldType.NORMAL;
        }
    }

    private String folderName(String id) {
        AriatusWorld world = worlds.get(id.toLowerCase());
        return world == null ? id : world.folder();
    }

    private boolean deleteFolder(File folder) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!deleteFolder(file)) {
                        return false;
                    }
                } else {
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
        }

        return folder.delete();
    }

    public Optional<WorldBorderData> border(String id) {
        return configManager.border(id);
    }

    public Optional<WorldBorderData> borderByBukkitWorld(String bukkitWorldName) {
        return worlds.values().stream()
                .filter(world -> world.folder().equalsIgnoreCase(bukkitWorldName))
                .findFirst()
                .flatMap(world -> border(world.id()));
    }

    public Optional<AriatusWorld> byBukkitWorldName(String bukkitWorldName) {
        return worlds.values().stream()
                .filter(world -> world.folder().equalsIgnoreCase(bukkitWorldName))
                .findFirst();
    }

    public void setRectangleBorder(
            String worldId,
            double minX,
            double maxX,
            double minZ,
            double maxZ
    ) {
        try {
            configManager.setRectangleBorder(worldId, minX, maxX, minZ, maxZ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}