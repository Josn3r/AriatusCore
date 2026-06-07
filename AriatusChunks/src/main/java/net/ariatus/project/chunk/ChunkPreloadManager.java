package net.ariatus.project.chunk;

import net.ariatus.project.AriatusChunks;
import net.ariatus.project.api.chunk.ChunkPreloadService;
import net.ariatus.project.api.chunk.ChunkPreloadState;
import net.ariatus.project.api.chunk.ChunkPreloadTaskView;
import net.ariatus.project.api.world.WorldService;
import net.ariatus.project.api.world.WorldView;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkPreloadManager implements ChunkPreloadService {

    private final AriatusChunks module;
    private final ChunkConfigManager configManager;
    private final Map<String, ChunkPreloadTask> tasks = new ConcurrentHashMap<>();

    public ChunkPreloadManager(AriatusChunks module, ChunkConfigManager configManager) {
        this.module = module;
        this.configManager = configManager;
        this.tasks.putAll(configManager.loadTasks());
    }

    public void startTicker() {
        module.tasks().runRepeating(module, this::tick, 20L, 1L);
    }

    @Override
    public boolean start(String worldId, int radius) {
        return start(worldId, 0, 0, radius);
    }

    @Override
    public boolean start(String worldId, int centerX, int centerZ, int radius) {
        worldId = worldId.toLowerCase();

        if (radius <= 0) {
            return false;
        }

        if (onlyOneTaskAtATime() && hasRunningTask()) {
            return false;
        }

        WorldService worldService = worldService();

        if (worldService == null) {
            module.logger().warn(module, "WorldService no está disponible. ¿AriatusWorlds está cargado?");
            return false;
        }

        Optional<WorldView> optionalWorld = worldService.world(worldId);

        if (optionalWorld.isEmpty()) {
            return false;
        }

        WorldView worldView = optionalWorld.get();

        worldService.load(worldId).join();

        World bukkitWorld = Bukkit.getWorld(worldView.folder());

        if (bukkitWorld == null) {
            return false;
        }

        ChunkPreloadTask task = ChunkPreloadTask.fresh(
                worldId,
                bukkitWorld.getName(),
                centerX,
                centerZ,
                radius
        );

        tasks.put(worldId, task);
        configManager.saveTask(task);

        module.logger().info(module, "Pregeneración iniciada para " + worldId + " con radio " + radius);
        return true;
    }

    @Override
    public boolean pause(String worldId) {
        ChunkPreloadTask task = tasks.get(worldId.toLowerCase());

        if (task == null || task.state() != ChunkPreloadState.RUNNING) {
            return false;
        }

        task.state(ChunkPreloadState.PAUSED);
        configManager.saveTask(task);
        return true;
    }

    @Override
    public boolean resume(String worldId) {
        worldId = worldId.toLowerCase();

        ChunkPreloadTask task = tasks.get(worldId);

        if (task == null || task.state() != ChunkPreloadState.PAUSED) {
            return false;
        }

        if (onlyOneTaskAtATime() && hasRunningTask()) {
            return false;
        }

        task.state(ChunkPreloadState.RUNNING);
        configManager.saveTask(task);
        return true;
    }

    @Override
    public boolean cancel(String worldId) {
        worldId = worldId.toLowerCase();

        ChunkPreloadTask task = tasks.get(worldId);

        if (task == null) {
            return false;
        }

        task.state(ChunkPreloadState.CANCELLED);
        configManager.saveTask(task);
        return true;
    }

    @Override
    public Optional<ChunkPreloadTaskView> task(String worldId) {
        return Optional.ofNullable(tasks.get(worldId.toLowerCase()));
    }

    public Optional<ChunkPreloadTask> internalTask(String worldId) {
        return Optional.ofNullable(tasks.get(worldId.toLowerCase()));
    }

    public void shutdown() {
        for (ChunkPreloadTask task : tasks.values()) {
            if (task.state() == ChunkPreloadState.RUNNING) {
                task.state(ChunkPreloadState.PAUSED);
                configManager.saveTask(task);
            }
        }
    }

    private void tick() {
        if (module.configBoolean("config.yml", "chunks.safety.auto-pause-when-no-players-online", false)
                && Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        int chunksPerTick = module.configInt("config.yml", "chunks.default.chunks-per-tick", 4);
        int saveEveryChunks = module.configInt("config.yml", "chunks.default.save-every-chunks", 250);
        boolean keepLoaded = module.configBoolean("config.yml", "chunks.default.keep-loaded", false);

        for (ChunkPreloadTask task : tasks.values()) {
            if (task.state() != ChunkPreloadState.RUNNING) {
                continue;
            }

            World world = Bukkit.getWorld(task.worldName());

            if (world == null) {
                task.state(ChunkPreloadState.FAILED);
                configManager.saveTask(task);
                continue;
            }

            processTask(world, task, chunksPerTick, saveEveryChunks, keepLoaded);

            if (onlyOneTaskAtATime()) {
                break;
            }
        }
    }

    private void processTask(
            World world,
            ChunkPreloadTask task,
            int chunksPerTick,
            int saveEveryChunks,
            boolean keepLoaded
    ) {
        for (int i = 0; i < chunksPerTick; i++) {
            if (task.state() != ChunkPreloadState.RUNNING) {
                return;
            }

            if (task.currentChunkZ() > task.maxChunkZ()) {
                complete(task);
                return;
            }

            try {
                Chunk chunk = world.getChunkAt(task.currentChunkX(), task.currentChunkZ());
                chunk.load(true);

                if (!keepLoaded) {
                    chunk.unload(true);
                }

                task.advance();

                if (saveEveryChunks > 0 && task.processedChunks() % saveEveryChunks == 0) {
                    configManager.saveTask(task);
                }

                if (task.state() == ChunkPreloadState.COMPLETED) {
                    complete(task);
                    return;
                }

            } catch (Exception exception) {
                module.logger().warn(
                        module,
                        "Error pregenerando chunk "
                                + task.currentChunkX()
                                + ", "
                                + task.currentChunkZ()
                                + " en "
                                + task.worldId()
                                + ": "
                                + exception.getMessage()
                );

                task.state(ChunkPreloadState.FAILED);
                configManager.saveTask(task);
                return;
            }
        }
    }

    private void complete(ChunkPreloadTask task) {
        task.state(ChunkPreloadState.COMPLETED);
        configManager.saveTask(task);

        World world = Bukkit.getWorld(task.worldName());

        if (world != null) {
            world.save();
        }

        module.logger().info(module, "Pregeneración completada para " + task.worldId());
    }

    @Override
    public Collection<ChunkPreloadTaskView> tasks() {
        return java.util.List.copyOf(tasks.values());
    }

    private boolean hasRunningTask() {
        return tasks.values().stream()
                .anyMatch(task -> task.state() == ChunkPreloadState.RUNNING);
    }

    private boolean onlyOneTaskAtATime() {
        return module.configBoolean("config.yml", "chunks.safety.only-run-one-task-at-a-time", true);
    }

    private WorldService worldService() {
        try {
            return module.services().require(WorldService.class);
        } catch (Exception exception) {
            return null;
        }
    }
}