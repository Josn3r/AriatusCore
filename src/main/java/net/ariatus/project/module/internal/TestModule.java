package net.ariatus.project.module.internal;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.command.AriatusCommandManager;
import net.ariatus.project.listener.AriatusListenerManager;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.task.AriatusTaskManager;

public class TestModule implements AriatusModule {

    private final AriatusCore core;
    private ModuleStatus status = ModuleStatus.DISABLED;

    public TestModule(AriatusCore core) {
        this.core = core;
    }

    @Override
    public String id() {
        return "test";
    }

    @Override
    public String name() {
        return "Test Module";
    }

    @Override
    public void enable() {
        status = ModuleStatus.ENABLING;

        AriatusTaskManager taskManager = core.services().require(AriatusTaskManager.class);
        AriatusListenerManager listenerManager = core.services().require(AriatusListenerManager.class);
        AriatusCommandManager commandManager = core.services().require(AriatusCommandManager.class);

        taskManager.runRepeating(this, () -> {
            core.loggerService().info("[TestModule] Task desde ServiceRegistry.");
        }, 20L, 20L * 30);

        listenerManager.register(this, new TestJoinListener());
        commandManager.register(this, new TestModuleCommand());

        status = ModuleStatus.ENABLED;
        core.loggerService().info("[TestModule] Activado.");
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLING;

        core.taskManager().cancelAll(this);
        core.listenerManager().unregisterAll(this);
        core.commandManager().unregisterAll(this);

        status = ModuleStatus.DISABLED;
        core.loggerService().info("[TestModule] Desactivado.");
    }

    @Override
    public void reload() {
        status = ModuleStatus.RELOADING;
        disable();
        enable();
        core.loggerService().info("[TestModule] Recargado.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }
}