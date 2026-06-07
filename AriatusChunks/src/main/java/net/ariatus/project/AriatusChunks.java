package net.ariatus.project;

import net.ariatus.project.api.chunk.ChunkPreloadService;
import net.ariatus.project.chunk.ChunkConfigManager;
import net.ariatus.project.chunk.ChunkPreloadManager;
import net.ariatus.project.commands.ChunksCommand;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;

public class AriatusChunks extends ExternalAriatusModule {

    private ModuleStatus status = ModuleStatus.DISABLED;

    private ChunkConfigManager configManager;
    private ChunkPreloadManager preloadManager;

    @Override
    public String id() {
        return "chunks";
    }

    @Override
    public String name() {
        return "AriatusChunks";
    }

    @Override
    public void enable() {
        status = ModuleStatus.ENABLING;

        loadConfig("config.yml");
        loadConfig("pregeneration.yml");

        this.configManager = new ChunkConfigManager(this);
        this.preloadManager = new ChunkPreloadManager(this, configManager);

        services().register(ChunkPreloadService.class, preloadManager);

        commands().register(this, new ChunksCommand(preloadManager));

        preloadManager.startTicker();

        status = ModuleStatus.ENABLED;
        logger().info(this, "AriatusChunks activado correctamente.");
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLING;

        if (preloadManager != null) {
            preloadManager.shutdown();
        }

        status = ModuleStatus.DISABLED;
        logger().info(this, "AriatusChunks desactivado correctamente.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }
}