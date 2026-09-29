package net.ariatus.project.module.runtime;

import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.command.AriatusCommandManager;
import net.ariatus.project.module.AriatusModule;

import java.util.Objects;
import java.util.Optional;

public final class ModuleCommands {

    private final AriatusModule module;
    private final AriatusCommandManager manager;

    public ModuleCommands(AriatusModule module, AriatusCommandManager manager) {
        this.module = Objects.requireNonNull(module, "module");
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public <T extends AriatusCommandExecutor> T register(T command) {
        return manager.register(module, command);
    }

    public Optional<AriatusCommandExecutor> find(String name) {
        return manager.getCommand(name);
    }

    public int active() {
        return manager.activeCommands(module);
    }
}