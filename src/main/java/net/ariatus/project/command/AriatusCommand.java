package net.ariatus.project.command;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.module.ModuleContainer;
import net.ariatus.project.module.ModuleDescriptor;
import net.ariatus.project.module.ModuleManager;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.profiler.ModuleProfileSnapshot;
import net.ariatus.project.profiler.ProfilerCategory;
import net.ariatus.project.profiler.ProfilerMetricSnapshot;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AriatusCommand implements CommandExecutor {

    private final ModuleManager moduleManager;
    private final AriatusCore core;

    public AriatusCommand(
            ModuleManager moduleManager
    ) {
        this.moduleManager =
                moduleManager;

        this.core =
                moduleManager.core();
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (
                !sender.hasPermission(
                        "ariatus.admin"
                )
        ) {
            MessageService.send(
                    sender,
                    "<red>No tienes permiso para administrar AriatusCore.</red>"
            );

            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action =
                args[0].toLowerCase(
                        Locale.ROOT
                );

        try {
            return switch (action) {
                case "help" -> {
                    sendHelp(sender);
                    yield true;
                }

                case "status" -> {
                    sendStatus(sender);
                    yield true;
                }

                case "modules" -> {
                    sendModules(sender);
                    yield true;
                }

                case "info" -> {
                    handleInfo(
                            sender,
                            args
                    );

                    yield true;
                }

                case "scan" -> {
                    handleScan(sender);
                    yield true;
                }

                case "load" -> {
                    handleLoad(
                            sender,
                            args
                    );

                    yield true;
                }

                case "enable" -> {
                    handleEnable(
                            sender,
                            args
                    );

                    yield true;
                }

                case "disable" -> {
                    handleDisable(
                            sender,
                            args
                    );

                    yield true;
                }

                case "reload" -> {
                    handleReload(
                            sender,
                            args
                    );

                    yield true;
                }

                case "unload" -> {
                    handleUnload(
                            sender,
                            args
                    );

                    yield true;
                }

                case "profiler" -> {
                    handleProfiler(
                            sender,
                            args
                    );

                    yield true;
                }

                case "profile" -> {
                    handleProfile(
                            sender,
                            args
                    );

                    yield true;
                }

                case "version" -> {
                    sendVersion(sender);
                    yield true;
                }

                default -> {
                    MessageService.send(
                            sender,
                            "<red>Subcomando desconocido:</red> <yellow>"
                                    + action
                                    + "</yellow>"
                    );

                    MessageService.send(
                            sender,
                            "<gray>Usa</gray> <aqua>/ariatus help</aqua><gray>.</gray>"
                    );

                    yield true;
                }
            };

        } catch (Exception exception) {
            core.loggerService()
                    .error(
                            "Error ejecutando /ariatus "
                                    + action
                                    + ".",
                            exception
                    );

            MessageService.send(
                    sender,
                    "<red>Ocurrió un error ejecutando la operación.</red> <gray>Revisa la consola.</gray>"
            );

            return true;
        }
    }

    private void sendHelp(
            CommandSender sender
    ) {
        MessageService.send(
                sender,
                ""
        );

        MessageService.send(
                sender,
                "<gradient:#8A2BE2:#00D4FF><bold>ARIATUSCORE 2.0</bold></gradient>"
        );

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus status</aqua> <gray>- Estado general.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus modules</aqua> <gray>- Lista de módulos.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus info <id></aqua> <gray>- Información de un módulo.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus profile <id></aqua> <gray>- Métricas detalladas.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus profiler</aqua> <gray>- Resumen del profiler.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus profiler reset [id]</aqua> <gray>- Reinicia métricas.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus scan</aqua> <gray>- Reescanea JARs.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus load <id></aqua> <gray>- Carga un módulo.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus enable <id></aqua> <gray>- Activa un módulo.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus disable <id></aqua> <gray>- Desactiva un módulo.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus reload <id></aqua> <gray>- Hard reload.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus unload <id></aqua> <gray>- Descarga completamente.</gray>"
        );

        MessageService.send(
                sender,
                "<aqua>/ariatus version</aqua> <gray>- Información de versión.</gray>"
        );

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        MessageService.send(
                sender,
                ""
        );
    }

    private void sendStatus(
            CommandSender sender
    ) {
        var containers =
                moduleManager.getContainers();

        long enabled =
                containers.stream()
                        .filter(
                                ModuleContainer::isEnabled
                        )
                        .count();

        long errors =
                containers.stream()
                        .filter(container ->
                                container.status()
                                        == ModuleStatus.ERROR
                        )
                        .count();

        int tasks =
                containers.stream()
                        .mapToInt(container ->
                                moduleManager.taskManager()
                                        .activeTasks(
                                                container.module()
                                        )
                        )
                        .sum();

        int listeners =
                containers.stream()
                        .mapToInt(container ->
                                moduleManager.listenerManager()
                                        .activeListeners(
                                                container.module()
                                        )
                        )
                        .sum();

        int commands =
                containers.stream()
                        .mapToInt(container ->
                                moduleManager.commandManager()
                                        .activeCommands(
                                                container.module()
                                        )
                        )
                        .sum();

        int services =
                containers.stream()
                        .mapToInt(container ->
                                core.services()
                                        .countOwned(
                                                container.module()
                                        )
                        )
                        .sum();

        int resources =
                containers.stream()
                        .mapToInt(container ->
                                container.module()
                                        .resources()
                                        .active()
                        )
                        .sum();

        int configs =
                containers.stream()
                        .mapToInt(container ->
                                core.moduleConfigManager()
                                        .loaded(
                                                container.module()
                                        )
                        )
                        .sum();

        MessageService.send(
                sender,
                ""
        );

        MessageService.send(
                sender,
                "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS STATUS</bold></gradient>"
        );

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        MessageService.send(
                sender,
                "<gray>Runtime:</gray> "
                        + runtimeText()
        );

        MessageService.send(
                sender,
                "<gray>Database:</gray> <yellow>"
                        + core.databaseService()
                        .status()
                        + "</yellow>"
        );

        MessageService.send(
                sender,
                "<gray>Descubiertos:</gray> <aqua>"
                        + core.moduleLoader()
                        .discoveredModules()
                        .size()
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Cargados:</gray> <aqua>"
                        + containers.size()
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Activos:</gray> <green>"
                        + enabled
                        + "</green>"
                        + " <dark_gray>|</dark_gray> "
                        + "<gray>Errores:</gray> <red>"
                        + errors
                        + "</red>"
        );

        MessageService.send(
                sender,
                "<gray>JARs fallidos:</gray> <yellow>"
                        + core.moduleLoader()
                        .failedModules()
                        .size()
                        + "</yellow>"
        );

        MessageService.send(
                sender,
                "<dark_gray>────────────────────────────────────</dark_gray>"
        );

        MessageService.send(
                sender,
                "<gray>Tasks:</gray> <aqua>"
                        + tasks
                        + "</aqua>"
                        + " <dark_gray>|</dark_gray> "
                        + "<gray>Listeners:</gray> <aqua>"
                        + listeners
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Commands:</gray> <aqua>"
                        + commands
                        + "</aqua>"
                        + " <dark_gray>|</dark_gray> "
                        + "<gray>Services:</gray> <aqua>"
                        + services
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Resources:</gray> <aqua>"
                        + resources
                        + "</aqua>"
                        + " <dark_gray>|</dark_gray> "
                        + "<gray>Configs:</gray> <aqua>"
                        + configs
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        MessageService.send(
                sender,
                ""
        );
    }

    private void sendModules(
            CommandSender sender
    ) {
        MessageService.send(
                sender,
                ""
        );

        MessageService.send(
                sender,
                "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS MODULES</bold></gradient>"
        );

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        var containers =
                moduleManager.getContainers()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        container ->
                                                container.descriptor()
                                                        .id()
                                )
                        )
                        .toList();

        if (containers.isEmpty()) {
            MessageService.send(
                    sender,
                    "<gray>No hay módulos cargados.</gray>"
            );

        } else {
            for (
                    ModuleContainer container :
                    containers
            ) {
                ModuleDescriptor descriptor =
                        container.descriptor();

                MessageService.send(
                        sender,
                        statusSymbol(
                                container.status()
                        )
                                + " <white>"
                                + descriptor.id()
                                + "</white>"
                                + " <dark_gray>v"
                                + descriptor.version()
                                + "</dark_gray> "
                                + statusText(
                                container.status()
                        )
                );
            }
        }

        List<ModuleDescriptor> unloaded =
                core.moduleLoader()
                        .discoveredModules()
                        .stream()
                        .filter(descriptor ->
                                !core.moduleLoader()
                                        .loadedModules()
                                        .containsKey(
                                                descriptor.id()
                                        )
                        )
                        .sorted(
                                Comparator.comparing(
                                        ModuleDescriptor::id
                                )
                        )
                        .toList();

        if (!unloaded.isEmpty()) {
            MessageService.send(
                    sender,
                    ""
            );

            MessageService.send(
                    sender,
                    "<gray>Detectados pero no cargados:</gray>"
            );

            for (
                    ModuleDescriptor descriptor :
                    unloaded
            ) {
                String failure =
                        core.moduleLoader()
                                .failedModules()
                                .get(
                                        descriptor.id()
                                );

                if (failure != null) {
                    MessageService.send(
                            sender,
                            "<red>!</red> <white>"
                                    + descriptor.id()
                                    + "</white> <red>"
                                    + failure
                                    + "</red>"
                    );

                } else {
                    MessageService.send(
                            sender,
                            "<dark_gray>○</dark_gray> <white>"
                                    + descriptor.id()
                                    + "</white> <dark_gray>v"
                                    + descriptor.version()
                                    + "</dark_gray> <gray>DISCOVERED</gray>"
                    );
                }
            }
        }

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        MessageService.send(
                sender,
                ""
        );
    }

    private void handleInfo(
            CommandSender sender,
            String[] args
    ) {
        if (args.length < 2) {
            MessageService.send(
                    sender,
                    "<yellow>Uso: /ariatus info <id></yellow>"
            );

            return;
        }

        String id =
                normalizeId(
                        args[1]
                );

        Optional<ModuleContainer> loaded =
                moduleManager.getContainer(id);

        if (loaded.isPresent()) {
            sendLoadedModuleInfo(
                    sender,
                    loaded.get()
            );

            return;
        }

        Optional<ModuleDescriptor> discovered =
                core.moduleLoader()
                        .discoveredModule(id);

        if (discovered.isPresent()) {
            sendDiscoveredModuleInfo(
                    sender,
                    discovered.get()
            );

            return;
        }

        MessageService.send(
                sender,
                "<red>Módulo no encontrado:</red> <yellow>"
                        + id
                        + "</yellow>"
        );
    }

    private void handleProfiler(
            CommandSender sender,
            String[] args
    ) {
        if (
                args.length >= 2
                        && args[1].equalsIgnoreCase(
                        "reset"
                )
        ) {
            if (args.length >= 3) {
                String id =
                        normalizeId(
                                args[2]
                        );

                core.profiler()
                        .reset(id);

                MessageService.send(
                        sender,
                        "<green>Métricas reiniciadas:</green> <yellow>"
                                + id
                                + "</yellow>"
                );

                return;
            }

            core.profiler()
                    .resetAll();

            MessageService.send(
                    sender,
                    "<green>Todas las métricas del profiler fueron reiniciadas.</green>"
            );

            return;
        }

        Set<String> ids =
                new LinkedHashSet<>();

        moduleManager.getContainers()
                .forEach(container ->
                        ids.add(
                                container.descriptor()
                                        .id()
                        )
                );

        ids.addAll(
                core.profiler()
                        .moduleIds()
        );

        List<ModuleProfileSnapshot> snapshots =
                ids.stream()
                        .map(
                                core.profiler()::snapshot
                        )
                        .sorted(
                                Comparator.comparingDouble(
                                        ModuleProfileSnapshot::mainThreadTotalMillis
                                )
                                        .reversed()
                        )
                        .toList();

        MessageService.send(
                sender,
                ""
        );

        MessageService.send(
                sender,
                "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS PROFILER</bold></gradient>"
        );

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        if (snapshots.isEmpty()) {
            MessageService.send(
                    sender,
                    "<gray>Todavía no existen métricas.</gray>"
            );

        } else {
            for (
                    ModuleProfileSnapshot snapshot :
                    snapshots
            ) {
                MessageService.send(
                        sender,
                        "<white>"
                                + snapshot.moduleId()
                                + "</white>"
                                + " <dark_gray>|</dark_gray> "
                                + "<gray>Main:</gray> <aqua>"
                                + decimal(
                                snapshot.mainThreadTotalMillis()
                        )
                                + "ms</aqua>"
                                + " <dark_gray>|</dark_gray> "
                                + "<gray>Total:</gray> <aqua>"
                                + decimal(
                                snapshot.totalMillis()
                        )
                                + "ms</aqua>"
                                + " <dark_gray>|</dark_gray> "
                                + "<gray>Calls:</gray> <yellow>"
                                + snapshot.totalExecutions()
                                + "</yellow>"
                                + " <dark_gray>|</dark_gray> "
                                + "<gray>Errors:</gray> <red>"
                                + snapshot.totalErrors()
                                + "</red>"
                );
            }
        }

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        MessageService.send(
                sender,
                "<gray>Usa</gray> <aqua>/ariatus profile <id></aqua> <gray>para el desglose.</gray>"
        );

        MessageService.send(
                sender,
                ""
        );
    }

    private void handleProfile(
            CommandSender sender,
            String[] args
    ) {
        if (args.length < 2) {
            MessageService.send(
                    sender,
                    "<yellow>Uso: /ariatus profile <id></yellow>"
            );

            return;
        }

        String id =
                normalizeId(
                        args[1]
                );

        Optional<ModuleContainer> container =
                moduleManager.getContainer(id);

        boolean historical =
                core.profiler()
                        .hasData(id);

        if (
                container.isEmpty()
                        && !historical
        ) {
            MessageService.send(
                    sender,
                    "<red>No existen métricas para:</red> <yellow>"
                            + id
                            + "</yellow>"
            );

            return;
        }

        ModuleProfileSnapshot snapshot =
                core.profiler()
                        .snapshot(id);

        MessageService.send(
                sender,
                ""
        );

        MessageService.send(
                sender,
                "<gradient:#8A2BE2:#00D4FF><bold>PROFILE: "
                        + id
                        + "</bold></gradient>"
        );

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        if (container.isPresent()) {
            ModuleContainer loaded =
                    container.get();

            MessageService.send(
                    sender,
                    "<gray>Estado:</gray> "
                            + statusText(
                            loaded.status()
                    )
            );

            MessageService.send(
                    sender,
                    "<gray>Tasks activas:</gray> <aqua>"
                            + moduleManager.taskManager()
                            .activeTasks(
                                    loaded.module()
                            )
                            + "</aqua>"
            );

            MessageService.send(
                    sender,
                    "<gray>Listeners:</gray> <aqua>"
                            + moduleManager.listenerManager()
                            .activeListeners(
                                    loaded.module()
                            )
                            + "</aqua>"
            );

            MessageService.send(
                    sender,
                    "<gray>Commands:</gray> <aqua>"
                            + moduleManager.commandManager()
                            .activeCommands(
                                    loaded.module()
                            )
                            + "</aqua>"
            );

        } else {
            MessageService.send(
                    sender,
                    "<gray>Estado:</gray> <dark_gray>UNLOADED</dark_gray>"
            );
        }

        MessageService.send(
                sender,
                "<gray>Ejecuciones:</gray> <yellow>"
                        + snapshot.totalExecutions()
                        + "</yellow>"
        );

        MessageService.send(
                sender,
                "<gray>Errores:</gray> <red>"
                        + snapshot.totalErrors()
                        + "</red>"
        );

        MessageService.send(
                sender,
                "<gray>Tiempo acumulado:</gray> <aqua>"
                        + decimal(
                        snapshot.totalMillis()
                )
                        + "ms</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Main thread acumulado:</gray> <aqua>"
                        + decimal(
                        snapshot.mainThreadTotalMillis()
                )
                        + "ms</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Main thread promedio:</gray> <aqua>"
                        + decimal(
                        snapshot.mainThreadAverageMillis()
                )
                        + "ms</aqua>"
        );

        MessageService.send(
                sender,
                "<dark_gray>────────────────────────────────────────────</dark_gray>"
        );

        for (
                ProfilerCategory category :
                ProfilerCategory.values()
        ) {
            ProfilerMetricSnapshot metric =
                    snapshot.metric(
                            category
                    );

            sendMetric(
                    sender,
                    category,
                    metric
            );
        }

        MessageService.send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        MessageService.send(
                sender,
                ""
        );
    }

    private void sendMetric(
            CommandSender sender,
            ProfilerCategory category,
            ProfilerMetricSnapshot metric
    ) {
        MessageService.send(
                sender,
                "<white><bold>"
                        + category.displayName()
                        + "</bold></white>"
        );

        MessageService.send(
                sender,
                "  <gray>Calls:</gray> <yellow>"
                        + metric.executions()
                        + "</yellow>"
                        + " <dark_gray>|</dark_gray> "
                        + "<gray>Errors:</gray> <red>"
                        + metric.errors()
                        + "</red>"
        );

        MessageService.send(
                sender,
                "  <gray>Avg:</gray> <aqua>"
                        + decimal(
                        metric.averageMillis()
                )
                        + "ms</aqua>"
                        + " <dark_gray>|</dark_gray> "
                        + "<gray>Max:</gray> <aqua>"
                        + decimal(
                        metric.maxMillis()
                )
                        + "ms</aqua>"
                        + " <dark_gray>|</dark_gray> "
                        + "<gray>Total:</gray> <aqua>"
                        + decimal(
                        metric.totalMillis()
                )
                        + "ms</aqua>"
        );

        if (
                metric.mainThreadExecutions()
                        > 0
        ) {
            MessageService.send(
                    sender,
                    "  <gray>Main:</gray> <aqua>"
                            + decimal(
                            metric.mainThreadTotalMillis()
                    )
                            + "ms</aqua>"
                            + " <dark_gray>|</dark_gray> "
                            + "<gray>Main Max:</gray> <aqua>"
                            + decimal(
                            metric.mainThreadMaxMillis()
                    )
                            + "ms</aqua>"
            );
        }
    }

    private void handleScan(
            CommandSender sender
    ) {
        core.moduleLoader()
                .discoverModules();

        MessageService.send(
                sender,
                "<green>Escaneo completado.</green>"
        );
    }

    private void handleLoad(
            CommandSender sender,
            String[] args
    ) {
        String id =
                requireModuleId(
                        sender,
                        args,
                        "load"
                );

        if (id == null) {
            return;
        }

        if (
                core.moduleLoader()
                        .loadedModules()
                        .containsKey(id)
        ) {
            MessageService.send(
                    sender,
                    "<yellow>El módulo ya está cargado:</yellow> <white>"
                            + id
                            + "</white>"
            );

            return;
        }

        if (
                !core.moduleLoader()
                        .isDiscovered(id)
        ) {
            MessageService.send(
                    sender,
                    "<red>El módulo no está en el catálogo actual:</red> <yellow>"
                            + id
                            + "</yellow>"
            );

            return;
        }

        if (
                core.moduleLoader()
                        .loadModuleById(id)
        ) {
            MessageService.send(
                    sender,
                    "<green>Módulo cargado:</green> <yellow>"
                            + id
                            + "</yellow>"
            );

        } else {
            sendOperationFailure(
                    sender,
                    id,
                    "cargar"
            );
        }
    }

    private void handleEnable(
            CommandSender sender,
            String[] args
    ) {
        String id =
                requireModuleId(
                        sender,
                        args,
                        "enable"
                );

        if (id == null) {
            return;
        }

        if (
                moduleManager.enable(id)
        ) {
            MessageService.send(
                    sender,
                    "<green>Módulo activado:</green> <yellow>"
                            + id
                            + "</yellow>"
            );

        } else {
            sendOperationFailure(
                    sender,
                    id,
                    "activar"
            );
        }
    }

    private void handleDisable(
            CommandSender sender,
            String[] args
    ) {
        String id =
                requireModuleId(
                        sender,
                        args,
                        "disable"
                );

        if (id == null) {
            return;
        }

        if (
                moduleManager.disable(id)
        ) {
            MessageService.send(
                    sender,
                    "<green>Módulo desactivado:</green> <yellow>"
                            + id
                            + "</yellow>"
            );

        } else {
            sendOperationFailure(
                    sender,
                    id,
                    "desactivar"
            );
        }
    }

    private void handleReload(
            CommandSender sender,
            String[] args
    ) {
        String id =
                requireModuleId(
                        sender,
                        args,
                        "reload"
                );

        if (id == null) {
            return;
        }

        if (
                core.moduleLoader()
                        .reloadModule(id)
        ) {
            MessageService.send(
                    sender,
                    "<green>Hard reload completado:</green> <yellow>"
                            + id
                            + "</yellow>"
            );

        } else {
            sendOperationFailure(
                    sender,
                    id,
                    "recargar"
            );
        }
    }

    private void handleUnload(
            CommandSender sender,
            String[] args
    ) {
        String id =
                requireModuleId(
                        sender,
                        args,
                        "unload"
                );

        if (id == null) {
            return;
        }

        if (
                core.moduleLoader()
                        .unloadModule(id)
        ) {
            MessageService.send(
                    sender,
                    "<green>Módulo descargado:</green> <yellow>"
                            + id
                            + "</yellow>"
            );

        } else {
            sendOperationFailure(
                    sender,
                    id,
                    "descargar"
            );
        }
    }

    private void sendLoadedModuleInfo(
            CommandSender sender,
            ModuleContainer container
    ) {
        ModuleDescriptor descriptor =
                container.descriptor();

        var module =
                container.module();

        MessageService.send(
                sender,
                ""
        );

        MessageService.send(
                sender,
                "<gradient:#8A2BE2:#00D4FF><bold>"
                        + descriptor.name()
                        + "</bold></gradient>"
        );

        MessageService.send(
                sender,
                "<gray>ID:</gray> <aqua>"
                        + descriptor.id()
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Versión:</gray> <aqua>"
                        + descriptor.version()
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Estado:</gray> "
                        + statusText(
                        container.status()
                )
        );

        MessageService.send(
                sender,
                "<gray>Dependencias:</gray> <white>"
                        + formatList(
                        descriptor.dependencies()
                )
                        + "</white>"
        );

        MessageService.send(
                sender,
                "<gray>Soft dependencies:</gray> <white>"
                        + formatList(
                        descriptor.softDependencies()
                )
                        + "</white>"
        );

        MessageService.send(
                sender,
                "<gray>Tasks:</gray> <aqua>"
                        + moduleManager.taskManager()
                        .activeTasks(module)
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Listeners:</gray> <aqua>"
                        + moduleManager.listenerManager()
                        .activeListeners(module)
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Commands:</gray> <aqua>"
                        + moduleManager.commandManager()
                        .activeCommands(module)
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Services:</gray> <aqua>"
                        + core.services()
                        .countOwned(module)
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Resources:</gray> <aqua>"
                        + module.resources()
                        .active()
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                ""
        );
    }

    private void sendDiscoveredModuleInfo(
            CommandSender sender,
            ModuleDescriptor descriptor
    ) {
        MessageService.send(
                sender,
                ""
        );

        MessageService.send(
                sender,
                "<gradient:#8A2BE2:#00D4FF><bold>"
                        + descriptor.name()
                        + "</bold></gradient>"
        );

        MessageService.send(
                sender,
                "<gray>ID:</gray> <aqua>"
                        + descriptor.id()
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Versión:</gray> <aqua>"
                        + descriptor.version()
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Estado:</gray> <gray>DISCOVERED</gray>"
        );

        MessageService.send(
                sender,
                "<gray>Dependencias:</gray> <white>"
                        + formatList(
                        descriptor.dependencies()
                )
                        + "</white>"
        );

        MessageService.send(
                sender,
                ""
        );
    }

    private void sendVersion(
            CommandSender sender
    ) {
        MessageService.send(
                sender,
                "<gradient:#8A2BE2:#00D4FF><bold>AriatusCore</bold></gradient> <aqua>"
                        + core.getPluginMeta()
                        .getVersion()
                        + "</aqua>"
        );

        MessageService.send(
                sender,
                "<gray>Minecraft:</gray> <yellow>"
                        + core.getServer()
                        .getMinecraftVersion()
                        + "</yellow>"
        );

        MessageService.send(
                sender,
                "<gray>Java:</gray> <yellow>"
                        + System.getProperty(
                        "java.version"
                )
                        + "</yellow>"
        );
    }

    private String requireModuleId(
            CommandSender sender,
            String[] args,
            String action
    ) {
        if (args.length < 2) {
            MessageService.send(
                    sender,
                    "<yellow>Uso: /ariatus "
                            + action
                            + " <id></yellow>"
            );

            return null;
        }

        return normalizeId(
                args[1]
        );
    }

    private void sendOperationFailure(
            CommandSender sender,
            String id,
            String operation
    ) {
        MessageService.send(
                sender,
                "<red>No se pudo "
                        + operation
                        + " el módulo</red> <yellow>"
                        + id
                        + "</yellow><red>.</red>"
        );

        String reason =
                core.moduleLoader()
                        .failedModules()
                        .get(id);

        if (reason != null) {
            MessageService.send(
                    sender,
                    "<gray>Motivo:</gray> <red>"
                            + reason
                            + "</red>"
            );
        }
    }

    private String normalizeId(
            String id
    ) {
        return id.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String decimal(
            double value
    ) {
        return String.format(
                Locale.US,
                "%.3f",
                value
        );
    }

    private String formatList(
            List<String> values
    ) {
        return values.isEmpty()
                ? "ninguna"
                : String.join(
                ", ",
                values
        );
    }

    private String statusSymbol(
            ModuleStatus status
    ) {
        return switch (status) {
            case ENABLED -> "<green>●</green>";
            case DISABLED -> "<gray>○</gray>";
            case ENABLING -> "<yellow>◐</yellow>";
            case DISABLING -> "<gold>◐</gold>";
            case ERROR -> "<red>●</red>";
        };
    }

    private String statusText(
            ModuleStatus status
    ) {
        return switch (status) {
            case ENABLED -> "<green>ENABLED</green>";
            case DISABLED -> "<gray>DISABLED</gray>";
            case ENABLING -> "<yellow>ENABLING</yellow>";
            case DISABLING -> "<gold>DISABLING</gold>";
            case ERROR -> "<red>ERROR</red>";
        };
    }

    private String runtimeText() {
        return switch (
                core.runtimeState()
        ) {
            case RUNNING -> "<green>RUNNING</green>";
            case STARTING -> "<yellow>STARTING</yellow>";
            case STOPPING -> "<gold>STOPPING</gold>";
            case FAILED -> "<red>FAILED</red>";
            case STOPPED -> "<gray>STOPPED</gray>";
        };
    }
}