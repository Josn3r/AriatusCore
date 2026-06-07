package net.ariatus.project.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class AriatusDynamicCommand extends Command {

    private final AriatusCommandExecutor executor;

    public AriatusDynamicCommand(AriatusCommandExecutor executor) {
        super(
                executor.name(),
                "Ariatus dynamic command",
                "/" + executor.name(),
                executor.aliases()
        );

        this.executor = executor;

        setPermission(null);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return executor.execute(sender, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
            throws IllegalArgumentException {

        List<String> completions = executor.tabComplete(sender, args);

        if (completions == null || completions.isEmpty()) {
            return List.of();
        }

        String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase();

        List<String> filtered = new ArrayList<>();

        for (String completion : completions) {
            if (completion == null || completion.isBlank()) {
                continue;
            }

            if (completion.toLowerCase().startsWith(current)) {
                filtered.add(completion);
            }
        }

        return filtered;
    }
}