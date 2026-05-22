package net.ariatus.project.command;

import net.ariatus.project.message.MessageService;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.module.ModuleManager;
import net.ariatus.project.module.ModuleStatus;
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

        if (!sender.hasPermission("ariatus.admin")) {
            MessageService.send(sender, "<red>No tienes permisos para usar este comando.</red>");
            return true;
        }

        if (args.length == 0) {
            MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>AriatusCore</bold></gradient> <gray>v0.1</gray>");
            MessageService.send(sender, "<yellow>/ariatus reload</yellow>");
            MessageService.send(sender, "<yellow>/ariatus health</yellow>");
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

            long start = System.nanoTime();

            try {
                return moduleCommand.get().execute(sender, subArgs);
            } catch (Exception exception) {
                moduleManager.core().loggerService().error("Error ejecutando comando de módulo: " + args[0] + " - " + exception.getMessage());
                return true;
            } finally {
                long elapsed = System.nanoTime() - start;
                moduleManager.core().profiler().record("command:" + args[0].toLowerCase(), elapsed);
            }
        }

        if (args[0].equalsIgnoreCase("reload")) {
            moduleManager.core().configManager().reload();
            moduleManager.core().messages().reload();
            MessageService.send(sender, "<green>Configuración de AriatusCore recargada.</green>");
            return true;
        }

        if (args[0].equalsIgnoreCase("version")) {
            var core = moduleManager.core();

            MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>AriatusCore</bold></gradient>");
            MessageService.send(sender, "<gray>Versión:</gray> <aqua>" + core.getPluginMeta().getVersion() + "</aqua>");
            MessageService.send(sender, "<gray>Servidor:</gray> <yellow>" + core.getServer().getName() + "</yellow>");
            MessageService.send(sender, "<gray>Minecraft:</gray> <yellow>" + core.getServer().getMinecraftVersion() + "</yellow>");
            MessageService.send(sender, "<gray>Java:</gray> <yellow>" + System.getProperty("java.version") + "</yellow>");

            return true;
        }

        if (args[0].equalsIgnoreCase("health")) {
            var core = moduleManager.core();

            MessageService.send(sender, "<gold>Estado de AriatusCore:</gold>");
            MessageService.send(sender, "<gray>Database:</gray> <yellow>" + core.databaseService().status() + "</yellow>");
            MessageService.send(sender, "<gray>Módulos registrados:</gray> <aqua>" + moduleManager.getModules().size() + "</aqua>");

            long enabledModules = moduleManager.getModules().stream()
                    .filter(module -> module.status().name().equalsIgnoreCase("ENABLED"))
                    .count();

            MessageService.send(sender, "<gray>Módulos activos:</gray> <aqua>" + enabledModules + "</aqua>");

            int totalTasks = moduleManager.getModules().stream()
                    .mapToInt(module -> moduleManager.taskManager().activeTasks(module))
                    .sum();

            int totalListeners = moduleManager.getModules().stream()
                    .mapToInt(module -> moduleManager.listenerManager().activeListeners(module))
                    .sum();

            int totalCommands = moduleManager.getModules().stream()
                    .mapToInt(module -> moduleManager.commandManager().activeCommands(module))
                    .sum();

            MessageService.send(sender, "<gray>Tasks activas:</gray> <aqua>" + totalTasks + "</aqua>");
            MessageService.send(sender, "<gray>Listeners activos:</gray> <aqua>" + totalListeners + "</aqua>");
            MessageService.send(sender, "<gray>Comandos de módulos:</gray> <aqua>" + totalCommands + "</aqua>");

            MessageService.send(sender, "<gray>Módulos externos cargados:</gray> <aqua>" + core.moduleLoader().loadedModules().size() + "</aqua>");

            return true;
        }

        if (args[0].equalsIgnoreCase("modules")) {
            var modules = moduleManager.getModules();

            var enabled = modules.stream()
                    .filter(module -> module.status() == ModuleStatus.ENABLED)
                    .toList();

            var disabled = modules.stream()
                    .filter(module -> module.status() == ModuleStatus.DISABLED)
                    .toList();

            var failed = moduleManager.core().moduleLoader().failedModules();

            MessageService.send(sender, "");
            MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS MODULES</bold></gradient>");
            MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
            MessageService.send(sender, "<gray>Total:</gray> <white>" + modules.size() + "</white> "
                    + "<dark_gray>|</dark_gray> <green>Activos:</green> <white>" + enabled.size() + "</white> "
                    + "<dark_gray>|</dark_gray> <red>Inactivos:</red> <white>" + disabled.size() + "</white> "
                    + "<dark_gray>|</dark_gray> <yellow>Fallidos:</yellow> <white>" + failed.size() + "</white>");

            MessageService.send(sender, "");

            MessageService.send(sender, "<green><bold>Habilitados (" + enabled.size() + ")</bold></green><gray>:</gray> "
                    + formatModules(enabled));

            MessageService.send(sender, "<red><bold>Desactivados (" + disabled.size() + ")</bold></red><gray>:</gray> "
                    + formatModules(disabled));

            if (!failed.isEmpty()) {
                MessageService.send(sender, "<yellow><bold>Fallidos (" + failed.size() + ")</bold></yellow><gray>:</gray> "
                        + formatFailedModules(failed));
            }

            MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
            MessageService.send(sender, "<gray>Usa</gray> <aqua>/ariatus module info <id></aqua> <gray>para ver detalles.</gray>");
            MessageService.send(sender, "");

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
                MessageService.send(sender, "<yellow>Uso: /ariatus module enable <id></yellow>");
                MessageService.send(sender, "<yellow>Uso: /ariatus module disable <id></yellow>");
                MessageService.send(sender, "<yellow>Uso: /ariatus module reload <id></yellow>");
                MessageService.send(sender, "<yellow>Uso: /ariatus module load <id></yellow>");
                MessageService.send(sender, "<yellow>Uso: /ariatus module unload <id></yellow>");
                MessageService.send(sender, "<yellow>Uso: /ariatus module info <id></yellow>");
                return true;
            }

            String action = args[1];
            String moduleId = args[2];

            boolean result;

            switch (action.toLowerCase()) {
                case "enable" -> result = moduleManager.enable(moduleId);
                case "disable" -> result = moduleManager.disable(moduleId);
                case "unload" -> {
                    result = moduleManager.core().moduleLoader().unloadModule(moduleId);
                }
                case "load" -> {
                    moduleManager.core().moduleLoader().discoverModules();
                    moduleManager.core().moduleLoader().loadModules();
                    result = moduleManager.getModule(moduleId).isPresent();
                }
                case "reload" -> {
                    result = moduleManager.core().moduleLoader().reloadModule(moduleId);
                }
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
                case "info" -> {
                    var optionalModule = moduleManager.getModule(moduleId);

                    if (optionalModule.isEmpty()) {
                        MessageService.send(sender, "<red>Módulo no encontrado: " + moduleId + "</red>");
                        return true;
                    }

                    var module = optionalModule.get();

                    MessageService.send(sender, "<gold>Información del módulo:</gold>");
                    MessageService.send(sender, "<gray>ID:</gray> <yellow>" + module.id() + "</yellow>");
                    MessageService.send(sender, "<gray>Nombre:</gray> <white>" + module.name() + "</white>");
                    MessageService.send(sender, "<gray>Estado:</gray> <aqua>" + module.status() + "</aqua>");
                    MessageService.send(sender, "<gray>Dependencias:</gray> <aqua>" + module.dependencies() + "</aqua>");
                    MessageService.send(sender, "<gray>Tasks:</gray> <aqua>" + moduleManager.taskManager().activeTasks(module) + "</aqua>");
                    MessageService.send(sender, "<gray>Listeners:</gray> <aqua>" + moduleManager.listenerManager().activeListeners(module) + "</aqua>");
                    MessageService.send(sender, "<gray>Comandos:</gray> <aqua>" + moduleManager.commandManager().activeCommands(module) + "</aqua>");

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

    private String formatModules(java.util.List<AriatusModule> modules) {
        if (modules.isEmpty()) {
            return "<dark_gray>Ninguno</dark_gray>";
        }

        return modules.stream()
                .map(module -> {
                    String version = resolveModuleVersion(module);
                    return "<white>" + module.name() + "</white> <dark_gray>(</dark_gray><aqua>v" + version + "</aqua><dark_gray>)</dark_gray>";
                })
                .collect(java.util.stream.Collectors.joining("<dark_gray>, </dark_gray>"));
    }

    private String formatFailedModules(java.util.Map<String, String> failedModules) {
        if (failedModules.isEmpty()) {
            return "<dark_gray>Ninguno</dark_gray>";
        }

        return failedModules.entrySet().stream()
                .map(entry -> "<white>" + entry.getKey() + "</white> <dark_gray>(</dark_gray><yellow>" + entry.getValue() + "</yellow><dark_gray>)</dark_gray>")
                .collect(java.util.stream.Collectors.joining("<dark_gray>, </dark_gray>"));
    }

    private String resolveModuleVersion(AriatusModule module) {
        var loadedModule = moduleManager.core().moduleLoader().loadedModules().get(module.id());

        if (loadedModule == null) {
            return "unknown";
        }

        return loadedModule.descriptor().version();
    }
}