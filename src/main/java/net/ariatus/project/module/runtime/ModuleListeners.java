package net.ariatus.project.module.runtime;

import net.ariatus.project.listener.AriatusListenerManager;
import net.ariatus.project.module.AriatusModule;
import org.bukkit.event.Listener;

import java.util.Objects;

public final class ModuleListeners {

    private final AriatusModule module;
    private final AriatusListenerManager manager;

    public ModuleListeners(AriatusModule module, AriatusListenerManager manager) {
        this.module = Objects.requireNonNull(module, "module");
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public <T extends Listener> T register(T listener) {
        manager.register(module, listener);
        return listener;
    }

    public int active() {
        return manager.activeListeners(module);
    }

    public void unregisterAll() {
        manager.unregisterAll(module);
    }
}