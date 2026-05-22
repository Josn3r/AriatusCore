package net.ariatus.project.command;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.*;

public class AriatusCommandManager {

    private final AriatusCore core;

    private final Map<String, AriatusCommandExecutor> commands = new HashMap<>();
    private final Map<String, List<String>> commandsByModule = new HashMap<>();
    private final Map<String, AriatusDynamicCommand> bukkitCommands = new HashMap<>();

    public AriatusCommandManager(AriatusCore core) {
        this.core = core;
    }

    public void register(AriatusModule module, AriatusCommandExecutor command) {
        String commandName = command.name().toLowerCase();
        String moduleId = module.id().toLowerCase();

        commands.put(commandName, command);

        for (String alias : command.aliases()) {
            commands.put(alias.toLowerCase(), command);
        }

        commandsByModule
                .computeIfAbsent(moduleId, id -> new ArrayList<>())
                .add(commandName);

        registerBukkitCommand(command);

        core.loggerService().info("[CommandManager] Comando registrado: /" + commandName);
    }

    public Optional<AriatusCommandExecutor> getCommand(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }

    public Collection<AriatusCommandExecutor> getCommands() {
        return commands.values();
    }

    public void unregisterAll(AriatusModule module) {
        String moduleId = module.id().toLowerCase();

        List<String> moduleCommands = commandsByModule.remove(moduleId);

        if (moduleCommands == null) {
            return;
        }

        for (String commandName : moduleCommands) {
            unregisterBukkitCommand(commandName);

            AriatusCommandExecutor executor = commands.remove(commandName);

            if (executor != null) {
                for (String alias : executor.aliases()) {
                    commands.remove(alias.toLowerCase());
                    unregisterBukkitCommand(alias.toLowerCase());
                }
            }
        }

        core.loggerService().info("[CommandManager] Comandos eliminados del módulo: " + moduleId);
    }

    public int activeCommands(AriatusModule module) {
        return commandsByModule
                .getOrDefault(module.id().toLowerCase(), List.of())
                .size();
    }

    public void unregisterAll() {
        for (String commandName : new ArrayList<>(bukkitCommands.keySet())) {
            unregisterBukkitCommand(commandName);
        }

        commands.clear();
        commandsByModule.clear();
        bukkitCommands.clear();
    }

    private void registerBukkitCommand(AriatusCommandExecutor executor) {
        try {
            CommandMap commandMap = getCommandMap();

            AriatusDynamicCommand dynamicCommand = new AriatusDynamicCommand(executor);

            commandMap.register("ariatus", dynamicCommand);

            bukkitCommands.put(executor.name().toLowerCase(), dynamicCommand);

            for (String alias : executor.aliases()) {
                bukkitCommands.put(alias.toLowerCase(), dynamicCommand);
            }

        } catch (Exception exception) {
            core.loggerService().error("No se pudo registrar comando /" + executor.name() + ": " + exception.getMessage());
        }
    }

    private void unregisterBukkitCommand(String commandName) {
        try {
            CommandMap commandMap = getCommandMap();

            if (!(commandMap instanceof SimpleCommandMap simpleCommandMap)) {
                return;
            }

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands =
                    (Map<String, org.bukkit.command.Command>) knownCommandsField.get(simpleCommandMap);

            knownCommands.remove(commandName.toLowerCase());
            knownCommands.remove("ariatus:" + commandName.toLowerCase());

            bukkitCommands.remove(commandName.toLowerCase());

        } catch (Exception exception) {
            core.loggerService().error("No se pudo desregistrar comando /" + commandName + ": " + exception.getMessage());
        }
    }

    private CommandMap getCommandMap() throws Exception {
        Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        commandMapField.setAccessible(true);
        return (CommandMap) commandMapField.get(Bukkit.getServer());
    }
}