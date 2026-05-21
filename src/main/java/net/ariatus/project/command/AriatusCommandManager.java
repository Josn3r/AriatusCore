package net.ariatus.project.command;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;

import java.util.*;

public class AriatusCommandManager {

    private final AriatusCore core;
    private final Map<String, AriatusModuleCommand> commands = new HashMap<>();
    private final Map<String, List<String>> commandsByModule = new HashMap<>();

    public AriatusCommandManager(AriatusCore core) {
        this.core = core;
    }

    public void register(AriatusModule module, AriatusModuleCommand command) {
        String commandName = command.name().toLowerCase();
        String moduleId = module.id().toLowerCase();

        commands.put(commandName, command);

        commandsByModule
                .computeIfAbsent(moduleId, id -> new ArrayList<>())
                .add(commandName);

        core.getLogger().info("[CommandManager] Comando registrado: /ariatus " + commandName);
    }

    public Optional<AriatusModuleCommand> getCommand(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }

    public Collection<AriatusModuleCommand> getCommands() {
        return commands.values();
    }

    public void unregisterAll(AriatusModule module) {
        String moduleId = module.id().toLowerCase();

        List<String> moduleCommands = commandsByModule.remove(moduleId);

        if (moduleCommands == null) {
            return;
        }

        for (String commandName : moduleCommands) {
            commands.remove(commandName);
        }

        core.getLogger().info("[CommandManager] Comandos eliminados del módulo: " + moduleId);
    }

    public int activeCommands(AriatusModule module) {
        return commandsByModule
                .getOrDefault(module.id().toLowerCase(), List.of())
                .size();
    }

    public void unregisterAll() {
        commands.clear();
        commandsByModule.clear();
    }
}