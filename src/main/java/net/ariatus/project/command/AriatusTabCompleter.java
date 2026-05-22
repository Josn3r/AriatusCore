package net.ariatus.project.command;

import net.ariatus.project.module.ModuleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class AriatusTabCompleter implements TabCompleter {

    private final ModuleManager moduleManager;

    public AriatusTabCompleter(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ariatus.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(args[0], List.of(
                    "modules",
                    "module",
                    "profiler",
                    "database",
                    "health",
                    "version",
                    "migrations",
                    "scanmodules",
                    "reload"
            ));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("module")) {
            return filter(args[1], List.of(
                    "enable",
                    "disable",
                    "reload",
                    "hotreload",
                    "load",
                    "unload",
                    "info",
                    "profile"
            ));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("module")) {
            List<String> modules = new ArrayList<>();

            moduleManager.getModules().forEach(module -> modules.add(module.id()));

            return filter(args[2], modules);
        }

        return List.of();
    }

    private List<String> filter(String input, List<String> options) {
        String lowerInput = input.toLowerCase();

        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerInput))
                .toList();
    }
}