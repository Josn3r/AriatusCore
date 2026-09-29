package net.ariatus.project.command;

import net.ariatus.project.module.ModuleDescriptor;
import net.ariatus.project.module.ModuleManager;
import net.ariatus.project.module.ModuleStatus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AriatusTabCompleter implements TabCompleter {

    private static final List<String> ROOT_COMMANDS =
            List.of(
                    "status",
                    "modules",
                    "info",
                    "profile",
                    "profiler",
                    "scan",
                    "load",
                    "enable",
                    "disable",
                    "reload",
                    "unload",
                    "version",
                    "help"
            );

    private final ModuleManager moduleManager;

    public AriatusTabCompleter(
            ModuleManager moduleManager
    ) {
        this.moduleManager =
                moduleManager;
    }

    @Override
    public List<String> onTabComplete(
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
            return List.of();
        }

        if (args.length == 1) {
            return filter(
                    args[0],
                    ROOT_COMMANDS
            );
        }

        if (args.length == 2) {
            String action =
                    args[0].toLowerCase(
                            Locale.ROOT
                    );

            return switch (action) {
                case "info", "profile" ->
                        filter(
                                args[1],
                                allKnownModuleIds()
                        );

                case "load" ->
                        filter(
                                args[1],
                                loadableModuleIds()
                        );

                case "enable" ->
                        filter(
                                args[1],
                                enableableModuleIds()
                        );

                case "disable" ->
                        filter(
                                args[1],
                                enabledModuleIds()
                        );

                case "reload", "unload" ->
                        filter(
                                args[1],
                                loadedModuleIds()
                        );

                case "profiler" ->
                        filter(
                                args[1],
                                List.of(
                                        "reset"
                                )
                        );

                default ->
                        List.of();
            };
        }

        if (
                args.length == 3
                        && args[0].equalsIgnoreCase(
                        "profiler"
                )
                        && args[1].equalsIgnoreCase(
                        "reset"
                )
        ) {
            return filter(
                    args[2],
                    profilerModuleIds()
            );
        }

        return List.of();
    }

    private List<String> allKnownModuleIds() {
        Set<String> ids =
                new LinkedHashSet<>();

        moduleManager.getContainers()
                .forEach(container ->
                        ids.add(
                                container.descriptor()
                                        .id()
                        )
                );

        moduleManager.core()
                .moduleLoader()
                .discoveredModules()
                .stream()
                .map(
                        ModuleDescriptor::id
                )
                .forEach(
                        ids::add
                );

        ids.addAll(
                moduleManager.core()
                        .profiler()
                        .moduleIds()
        );

        return ids.stream()
                .sorted()
                .toList();
    }

    private List<String> profilerModuleIds() {
        Set<String> ids =
                new LinkedHashSet<>(
                        moduleManager.core()
                                .profiler()
                                .moduleIds()
                );

        moduleManager.getContainers()
                .forEach(container ->
                        ids.add(
                                container.descriptor()
                                        .id()
                        )
                );

        return ids.stream()
                .sorted()
                .toList();
    }

    private List<String> loadedModuleIds() {
        return moduleManager.getContainers()
                .stream()
                .map(container ->
                        container.descriptor()
                                .id()
                )
                .sorted()
                .toList();
    }

    private List<String> enabledModuleIds() {
        return moduleManager.getContainers()
                .stream()
                .filter(container ->
                        container.status()
                                == ModuleStatus.ENABLED
                )
                .map(container ->
                        container.descriptor()
                                .id()
                )
                .sorted()
                .toList();
    }

    private List<String> enableableModuleIds() {
        return moduleManager.getContainers()
                .stream()
                .filter(container ->
                        container.status()
                                != ModuleStatus.ENABLED
                )
                .map(container ->
                        container.descriptor()
                                .id()
                )
                .sorted()
                .toList();
    }

    private List<String> loadableModuleIds() {
        Set<String> loaded =
                new LinkedHashSet<>(
                        loadedModuleIds()
                );

        return moduleManager.core()
                .moduleLoader()
                .discoveredModules()
                .stream()
                .map(
                        ModuleDescriptor::id
                )
                .filter(id ->
                        !loaded.contains(id)
                )
                .sorted()
                .toList();
    }

    private List<String> filter(
            String input,
            List<String> options
    ) {
        String normalized =
                input.toLowerCase(
                        Locale.ROOT
                );

        return options.stream()
                .filter(option ->
                        option.toLowerCase(
                                        Locale.ROOT
                                )
                                .startsWith(
                                        normalized
                                )
                )
                .toList();
    }
}