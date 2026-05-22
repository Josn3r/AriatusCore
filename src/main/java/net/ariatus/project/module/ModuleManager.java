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
                core.loggerService().warn("Dependencia no encontrada: " + dependencyId + " requerida por " + module.id());
                return false;
            }

            if (dependency.get().status() != ModuleStatus.ENABLED) {
                if (!enable(dependencyId)) {
                    return false;
                }
            }
        }

        if (module.status() == ModuleStatus.ENABLED) {
            return true;
        }

        try {
            module.enable();
            core.eventBus().publish(new ModuleEnabledEvent(module));
            return true;
        } catch (Exception exception) {
            core.loggerService().error("Error activando módulo " + module.id() + ": " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    public boolean disable(String id) {
        Optional<AriatusModule> optional = getModule(id);

        if (optional.isEmpty()) {
            return false;
        }

        AriatusModule module = optional.get();

        for (AriatusModule other : modules.values()) {
            if (other.status() == ModuleStatus.ENABLED && other.dependencies().contains(module.id())) {
                core.loggerService().warn("No puedes desactivar " + module.id() + " porque lo requiere " + other.id());
                return false;
            }
        }

        if (module.status() == ModuleStatus.DISABLED) {
            return true;
        }

        try {
            module.disable();

            core.taskManager().cancelAll(module);
            core.listenerManager().unregisterAll(module);
            core.commandManager().unregisterAll(module);

            core.eventBus().publish(new ModuleDisabledEvent(module));
            return true;
        } catch (Exception exception) {
            core.loggerService().error("Error desactivando módulo " + module.id() + ": " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    public boolean reload(String id) {
        Optional<AriatusModule> optional = getModule(id);

        if (optional.isEmpty()) {
            return false;
        }

        AriatusModule module = optional.get();

        try {
            boolean disabled = disable(id);

            if (!disabled) {
                return false;
            }

            boolean enabled = enable(id);

            if (!enabled) {
                return false;
            }

            core.eventBus().publish(new ModuleReloadedEvent(module));
            return true;

        } catch (Exception exception) {
            core.loggerService().error("Error recargando módulo " + module.id() + ": " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    public boolean unregister(String id) {
        String moduleId = id.toLowerCase();

        AriatusModule module = modules.get(moduleId);

        if (module == null) {
            return false;
        }

        for (AriatusModule other : modules.values()) {
            if (other.status() == ModuleStatus.ENABLED && other.dependencies().contains(module.id())) {
                core.loggerService().warn("No puedes descargar " + module.id() + " porque lo requiere " + other.id());
                return false;
            }
        }

        if (module.status() == ModuleStatus.ENABLED) {
            module.disable();
        }

        modules.remove(moduleId);
        core.loggerService().info("Módulo eliminado del ModuleManager: " + moduleId);
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