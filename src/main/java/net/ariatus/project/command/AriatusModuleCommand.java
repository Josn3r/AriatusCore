package net.ariatus.project.command;

import org.bukkit.command.CommandSender;

public interface AriatusModuleCommand {

    String name();

    String description();

    String usage();

    boolean execute(CommandSender sender, String[] args);
}