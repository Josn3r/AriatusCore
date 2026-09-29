package net.ariatus.project.listener;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AriatusListenerManager {

    private final AriatusCore core;

    private final Map<String, Set<Listener>> listenersByModule =
            new ConcurrentHashMap<>();

    public AriatusListenerManager(AriatusCore core) {
        this.core = Objects.requireNonNull(core, "core");
    }

    public <T extends Listener> T register(AriatusModule module, T listener) {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(listener, "listener");

        String moduleId = module.id().toLowerCase(Locale.ROOT);

        core.getServer()
                .getPluginManager()
                .registerEvents(listener, core);

        listenersByModule
                .computeIfAbsent(
                        moduleId,
                        ignored -> ConcurrentHashMap.newKeySet()
                )
                .add(listener);

        return listener;
    }

    public void unregisterAll(AriatusModule module) {
        Objects.requireNonNull(module, "module");

        String moduleId = module.id().toLowerCase(Locale.ROOT);

        Set<Listener> listeners = listenersByModule.remove(moduleId);

        if (listeners == null) {
            return;
        }

        listeners.forEach(HandlerList::unregisterAll);
    }

    public int activeListeners(AriatusModule module) {
        return listenersByModule
                .getOrDefault(
                        module.id().toLowerCase(Locale.ROOT),
                        Set.of()
                )
                .size();
    }

    public void unregisterAll() {
        listenersByModule.values()
                .forEach(listeners ->
                        listeners.forEach(HandlerList::unregisterAll)
                );

        listenersByModule.clear();
    }
}