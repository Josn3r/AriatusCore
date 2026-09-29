package net.ariatus.project.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface AriatusCommandExecutor {

    String name();

    default List<String> aliases() {
        return List.of();
    }

    default String description() {
        return "";
    }

    default String usage() {
        return "/" + name();
    }

    default String permission() {
        return "";
    }

    boolean execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}