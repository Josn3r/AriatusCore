package net.ariatus.project.chunk;

import net.ariatus.project.AriatusChunks;
import net.ariatus.project.api.chunk.ChunkPreloadState;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChunkConfigManager {

    private static final String FILE = "pregeneration.yml";

    private final AriatusChunks module;

    public ChunkConfigManager(AriatusChunks module) {
        this.module = module;
    }

    public Map<String, ChunkPreloadTask> loadTasks() {
        Map<String, ChunkPreloadTask> tasks = new HashMap<>();

        ConfigurationSection section = module.config(FILE).getConfigurationSection("tasks");

        if (section == null) {
            return tasks;
        }

        for (String worldId : section.getKeys(false)) {
            String path = "tasks." + worldId + ".";

            try {
                ChunkPreloadTask task = new ChunkPreloadTask(
                        worldId.toLowerCase(),
                        module.configString(FILE, path + "world-name", worldId),
                        module.configInt(FILE, path + "center-x", 0),
                        module.configInt(FILE, path + "center-z", 0),
                        module.configInt(FILE, path + "radius", 1000),
                        module.configInt(FILE, path + "current-chunk-x", 0),
                        module.configInt(FILE, path + "current-chunk-z", 0),
                        module.config(FILE).getLong(path + "processed-chunks", 0),
                        ChunkPreloadState.valueOf(
                                module.configString(FILE, path + "state", "PAUSED").toUpperCase()
                        )
                );

                tasks.put(task.worldId(), task);
            } catch (Exception exception) {
                module.logger().warn(module, "No se pudo cargar tarea de pregeneración: " + worldId);
            }
        }

        return tasks;
    }

    public void saveTask(ChunkPreloadTask task) {
        String path = "tasks." + task.worldId() + ".";

        module.config(FILE).set(path + "world-name", task.worldName());
        module.config(FILE).set(path + "center-x", task.centerX());
        module.config(FILE).set(path + "center-z", task.centerZ());
        module.config(FILE).set(path + "radius", task.radius());
        module.config(FILE).set(path + "current-chunk-x", task.currentChunkX());
        module.config(FILE).set(path + "current-chunk-z", task.currentChunkZ());
        module.config(FILE).set(path + "processed-chunks", task.processedChunks());
        module.config(FILE).set(path + "total-chunks", task.totalChunks());
        module.config(FILE).set(path + "state", task.state().name());

        module.saveConfig(FILE);
    }

    public void deleteTask(String worldId) {
        module.config(FILE).set("tasks." + worldId.toLowerCase(), null);
        module.saveConfig(FILE);
    }
}