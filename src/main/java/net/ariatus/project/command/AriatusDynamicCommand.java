package net.ariatus.project.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class AriatusDynamicCommand extends Command {

    private final AriatusCommandExecutor executor;

    public AriatusDynamicCommand(AriatusCommandExecutor executor) {
        super(
                executor.name(),
                "Ariatus module command",
                "/" + executor.name(),
                executor.aliases()
        );

        this.executor = executor;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        return executor.execute(sender, args);
    }
}