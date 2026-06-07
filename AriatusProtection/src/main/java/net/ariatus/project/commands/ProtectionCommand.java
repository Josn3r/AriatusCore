package net.ariatus.project.commands;

import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.protection.ProtectionManager;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ProtectionCommand implements AriatusCommandExecutor {

    private final ProtectionManager protectionManager;

    public ProtectionCommand(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }

    @Override
    public String name() {
        return "ariatusprotection";
    }

    @Override
    public List<String> aliases() {
        return List.of("aprotection", "protect", "ap");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ariatusprotection.admin")) {
            MessageService.send(sender, "&cNo tienes permisos.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> reload(sender);
            case "status" -> status(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void reload(CommandSender sender) {
        protectionManager.load();
        MessageService.send(sender, "&aAriatusProtection recargado correctamente.");
    }

    private void status(CommandSender sender) {
        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>ARIATUS PROTECTION</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7Mundos configurados: &e" + protectionManager.loadedWorlds());
        MessageService.send(sender, "&7Bypass permission: &f" + protectionManager.bypassPermission());
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void sendHelp(CommandSender sender) {
        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>ARIATUS PROTECTION</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&e/ap status &7- Muestra estado del módulo.");
        MessageService.send(sender, "&e/ap reload &7- Recarga la configuración.");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }
}