package net.ariatus.project.listener;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.*;

public class AriatusListenerManager {

    private final AriatusCore core;
    private final Map<String, List<Listener>> listenersByModule = new HashMap<>();

    public AriatusListenerManager(AriatusCore core) {
        this.core = core;
    }

    public void register(AriatusModule module, Listener listener) {
        String moduleId = module.id().toLowerCase();

        core.getServer().getPluginManager().registerEvents(listener, core);

        listenersByModule
                .computeIfAbsent(moduleId, id -> new ArrayList<>())
                .add(listener);

        core.getLogger().info("[ListenerManager] Listener registrado para módulo: " + moduleId);
    }

    public void unregisterAll(AriatusModule module) {
        String moduleId = module.id().toLowerCase();

        List<Listener> listeners = listenersByModule.remove(moduleId);

        if (listeners == null) {
            return;
        }

        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }

        core.getLogger().info("[ListenerManager] Listeners eliminados del módulo: " + moduleId);
    }

    public int activeListeners(AriatusModule module) {
        return listenersByModule
                .getOrDefault(module.id().toLowerCase(), List.of())
                .size();
    }

    public void unregisterAll() {
        for (List<Listener> listeners : listenersByModule.values()) {
            for (Listener listener : listeners) {
                HandlerList.unregisterAll(listener);
            }
        }

        listenersByModule.clear();
    }
}