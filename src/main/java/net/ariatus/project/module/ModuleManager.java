package net.ariatus.project.module;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.command.AriatusCommandManager;
import net.ariatus.project.event.ModuleDisabledEvent;
import net.ariatus.project.event.ModuleEnabledEvent;
import net.ariatus.project.event.ModuleReloadedEvent;
import net.ariatus.project.listener.AriatusListenerManager;
import net.ariatus.project.task.AriatusTaskManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ModuleManager {

    private final AriatusCore core;
    private final Map<String, AriatusModule> modules = new LinkedHashMap<>();

    public ModuleManager(AriatusCore core) {
        this.core = core;
    }

    public void register(AriatusModule module) {
        modules.put(module.id().toLowerCase(), module);
        core.getLogger().info("Módulo registrado: " + module.name());
    }

    public Optional<AriatusModule> getModule(String id) {
        return Optional.ofNullable(modules.get(id.toLowerCase()));
    }

    public Collection<AriatusModule> getModules() {
        return modules.values();
    }

    public boolean enable(String id) {
        Optional<AriatusModule> optional = getModule(id);

        if (optional.isEmpty()) {
            return false;
        }

        AriatusModule module = optional.get();

        for (String dependencyId : module.dependencies()) {
            Optional<AriatusModule> dependency = getModule(dependencyId);

            if (dependency.isEmpty()) {
                core.getLogger().warning("Dependencia no encontrada: " + dependencyId + " requerida por " + module.id());
                return false;
            }

            if (dependency.get().status() != ModuleStatus.ENABLED) {
                core.getLogger().info("Activando dependencia: " + dependencyId);
                enable(dependencyId);
            }
        }

        if (module.status() == ModuleStatus.ENABLED) {
            return true;
        }

        module.enable();
        core.eventBus().publish(new ModuleEnabledEvent(module));
        return true;
    }

    public boolean disable(String id) {
        Optional<AriatusModule> optional = getModule(id);

        if (optional.isEmpty()) {
            return false;
        }

        AriatusModule module = optional.get();

        for (AriatusModule other : modules.values()) {
            if (other.status() == ModuleStatus.ENABLED && other.dependencies().contains(module.id())) {
                core.getLogger().warning("No puedes desactivar " + module.id() + " porque lo requiere " + other.id());
                return false;
            }
        }

        if (module.status() == ModuleStatus.DISABLED) {
            return true;
        }

        module.disable();
        core.eventBus().publish(new ModuleDisabledEvent(module));
        return true;
    }

    public boolean reload(String id) {
        Optional<AriatusModule> optional = getModule(id);

        if (optional.isEmpty()) {
            return false;
        }

        AriatusModule module = optional.get();

        module.reload();
        core.eventBus().publish(new ModuleReloadedEvent(module));

        return true;
    }

    public void disableAll() {
        for (AriatusModule module : modules.values()) {
            if (module.status() == ModuleStatus.ENABLED) {
                module.disable();
            }
        }
    }

    public AriatusTaskManager taskManager() {
        return core.taskManager();
    }

    public AriatusListenerManager listenerManager() {
        return core.listenerManager();
    }

    public AriatusCommandManager commandManager() {
        return core.commandManager();
    }

    public AriatusCore core() {
        return core;
    }

}