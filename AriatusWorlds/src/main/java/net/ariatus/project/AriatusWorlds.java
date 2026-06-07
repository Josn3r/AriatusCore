package net.ariatus.project;

import net.ariatus.project.api.world.WorldService;
import net.ariatus.project.commands.WorldCommand;
import net.ariatus.project.listener.WorldBorderListener;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.world.WorldConfigManager;
import net.ariatus.project.world.WorldManager;

public class AriatusWorlds extends ExternalAriatusModule {

    private ModuleStatus status = ModuleStatus.DISABLED;

    private WorldConfigManager worldConfigManager;
    private WorldManager worldManager;

    @Override
    public String id() {
        return "worlds";
    }

    @Override
    public String name() {
        return "AriatusWorlds";
    }

    @Override
    public void enable() {
        status = ModuleStatus.ENABLING;

        loadConfig("config.yml");
        loadConfig("worlds.yml");

        this.worldConfigManager = new WorldConfigManager(this);
        this.worldManager = new WorldManager(this, worldConfigManager);

        services().register(WorldService.class, worldManager);
        listeners().register(this, new WorldBorderListener(worldManager));
        commands().register(this, new WorldCommand(worldManager));

        worldManager.autoLoadWorlds();

        status = ModuleStatus.ENABLED;
        logger().info(this, "AriatusWorlds activado correctamente.");
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLING;

        status = ModuleStatus.DISABLED;
        logger().info(this, "AriatusWorlds desactivado correctamente.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }
}