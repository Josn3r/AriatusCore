package net.ariatus.project.commands;

import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.storage.StorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class StorageCommand implements AriatusCommandExecutor {

    private final StorageManager storageManager;

    public StorageCommand(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    public String name() {
        return "storage";
    }

    @Override
    public List<String> aliases() {
        return List.of("baules", "vaults", "chests");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            storageManager.openMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("chest") || args[0].equalsIgnoreCase("openchest")) {
            if (args.length < 2) {
                MessageService.send(sender, "&cUso: &e/storage chest <número>");
                return true;
            }

            int chestNumber = parseInt(args[1], -1);

            if (chestNumber <= 0) {
                MessageService.send(sender, "&cNúmero de baúl inválido.");
                return true;
            }

            storageManager.openChest(player, chestNumber);
            return true;
        }

        MessageService.send(sender, "&cUso: &e/storage &7o &e/storage chest <número>");
        return true;
    }

    private int parseInt(String value, int def) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return def;
        }
    }
}