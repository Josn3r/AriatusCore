package net.ariatus.project.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface AriatusCommandExecutor {

    String name();

    default List<String> aliases() {
        return List.of();
    }

    boolean execute(CommandSender sender, String[] args);
}