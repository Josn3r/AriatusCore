package net.ariatus.project.command;

import net.ariatus.project.message.MessageService;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.module.ModuleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public class AriatusCommand implements CommandExecutor {

    private final ModuleManager moduleManager;

    public AriatusCommand(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {

        if (args.length == 0) {
            MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>AriatusCore</bold></gradient> <gray>v0.1</gray>");
            MessageService.send(sender, "<yellow>/ariatus profiler</yellow>");
            MessageService.send(sender, "<yellow>/ariatus database</yellow>");
            MessageService.send(sender, "<yellow>/ariatus migrations</yellow>");
            MessageService.send(sender, "<yellow>/ariatus modules</yellow>");
            MessageService.send(sender, "<yellow>/ariatus scanmodules</yellow>");
            MessageService.send(sender, "<yellow>/ariatus module enable <id></yellow>");
            MessageService.send(sender, "<yellow>/ariatus module disable <id></yellow>");
            MessageService.send(sender, "<yellow>/ariatus module reload <id></yellow>");
            return true;
        }

        var moduleCommand = moduleManager.commandManager().getCommand(args[0]);

        if (moduleCommand.isPresent()) {
            String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
            return moduleCommand.get().execute(sender, subArgs);
        }

        if (args[0].equalsIgnoreCase("modules")) {
            MessageService.send(sender, "<gold>Módulos Ariatus:</gold>");

            for (AriatusModule module : moduleManager.getModules()) {
                MessageService.send(sender,
                        "<yellow>- " + module.id() + "</yellow>" +
                                " <gray>|</gray> " +
                                "<white>" + module.name() + "</white>" +
                                " <gray>|</gray> " +
                                "<aqua>" + module.status() + "</aqua>"
                );
            }

            MessageService.send(sender, "<gold>Módulos externos detectados:</gold>");
            for (var descriptor : moduleManager.core().moduleLoader().discoveredModules()) {
                MessageService.send(sender,
                        "<yellow>- " + descriptor.id() + "</yellow>" +
                                " <gray>|</gray> <white>" + descriptor.name() + "</white>" +
                                " <gray>| v</gray><aqua>" + descriptor.version() + "</aqua>" +
                                " <gray>| main:</gray> <aqua>" + descriptor.main() + "</aqua>"
                );
            }

            MessageService.send(sender, "<gold>Módulos externos cargados:</gold>");
            for (var entry : moduleManager.core().moduleLoader().loadedModules().entrySet()) {
                var module = entry.getValue();
                MessageService.send(sender,
                        "<yellow>- " + module.id() + "</yellow>" +
                                " <gray>|</gray> " +
                                "<white>" + module.name() + "</white>" +
                                " <gray>|</gray> " +
                                "<aqua>" + module.status() + "</aqua>"
                );
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("scanmodules")) {
            moduleManager.core().moduleLoader().discoverModules();
            MessageService.send(sender, "<green>Escaneo de módulos completado.</green>");
            return true;
        }

        if (args[0].equalsIgnoreCase("profiler")) {
            MessageService.send(sender, "<gold>Profiler de módulos:</gold>");

            for (var entry : moduleManager.core().profiler().all().entrySet()) {
                var moduleId = entry.getKey();
                var metric = entry.getValue();

                MessageService.send(sender,
                        "<yellow>" + moduleId + "</yellow>" +
                                " <gray>| ejecuciones:</gray> <aqua>" + metric.executions() + "</aqua>" +
                                " <gray>| media:</gray> <aqua>" + String.format("%.3f", metric.averageMillis()) + "ms</aqua>" +
                                " <gray>| max:</gray> <aqua>" + String.format("%.3f", metric.maxMillis()) + "ms</aqua>" +
                                " <gray>| errores:</gray> <red>" + metric.errors() + "</red>"
                );
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("database")) {
            var database = moduleManager.core().databaseService();

            MessageService.send(sender, "<gold>Database status:</gold> <yellow>" + database.status() + "</yellow>");

            database.testConnection().thenAccept(success -> {
                if (success) {
                    MessageService.send(sender, "<green>Conexión SQL válida.</green>");
                } else {
                    MessageService.send(sender, "<red>No se pudo validar la conexión SQL.</red>");
                }
            });

            return true;
        }

        if (args[0].equalsIgnoreCase("migrations")) {
            moduleManager.core().migrationManager().runMigrations();
            MessageService.send(sender, "<green>Migraciones ejecutándose en segundo plano.</green>");
            return true;
        }

        if (args[0].equalsIgnoreCase("module")) {
            if (args.length < 3) {
                MessageService.send(sender, "<red>Uso: /ariatus module <enable|disable|reload> <id></red>");
                return true;
            }

            String action = args[1];
            String moduleId = args[2];

            boolean result;

            switch (action.toLowerCase()) {
                case "enable" -> result = moduleManager.enable(moduleId);
                case "disable" -> result = moduleManager.disable(moduleId);
                case "reload" -> result = moduleManager.reload(moduleId);
                case "profile" -> {
                    var optionalModule = moduleManager.getModule(moduleId);

                    if (optionalModule.isEmpty()) {
                        MessageService.send(sender, "<red>Módulo no encontrado: " + moduleId + "</red>");
                        return true;
                    }

                    var module = optionalModule.get();
                    var metrics = moduleManager.taskManager().metrics(module);

                    MessageService.send(sender, "<gold>Perfil del módulo:</gold> <yellow>" + module.id() + "</yellow>");
                    MessageService.send(sender, "<gray>Tasks activas:</gray> <aqua>" + moduleManager.taskManager().activeTasks(module) + "</aqua>");
                    MessageService.send(sender, "<gray>Listeners activos:</gray> <aqua>" + moduleManager.listenerManager().activeListeners(module) + "</aqua>");
                    MessageService.send(sender, "<gray>Comandos activos:</gray> <aqua>" + moduleManager.commandManager().activeCommands(module) + "</aqua>");
                    MessageService.send(sender, "<gray>Ejecuciones:</gray> <aqua>" + metrics.executions() + "</aqua>");
                    MessageService.send(sender, "<gray>Media:</gray> <aqua>" + String.format("%.3f", metrics.averageMillis()) + "ms</aqua>");
                    MessageService.send(sender, "<gray>Máximo:</gray> <aqua>" + String.format("%.3f", metrics.maxMillis()) + "ms</aqua>");
                    MessageService.send(sender, "<gray>Errores:</gray> <red>" + metrics.errors() + "</red>");
                    return true;
                }
                default -> {
                    MessageService.send(sender, "<red>Acción desconocida.</red>");
                    return true;
                }
            }

            if (!result) {
                MessageService.send(sender, "<red>Módulo no encontrado: " + moduleId + "</red>");
                return true;
            }

            MessageService.send(sender, "<green>Acción ejecutada sobre módulo:</green> <yellow>" + moduleId + "</yellow>");
            return true;
        }

        MessageService.send(sender, "<red>Comando desconocido.</red>");
        return true;
    }
}