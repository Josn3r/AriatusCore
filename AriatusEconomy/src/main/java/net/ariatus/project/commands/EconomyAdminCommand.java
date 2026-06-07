package net.ariatus.project.commands;

import net.ariatus.project.api.economy.Currency;
import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.economy.EconomyManager;
import net.ariatus.project.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.math.BigDecimal;
import java.util.List;

public class EconomyAdminCommand implements AriatusCommandExecutor {

    private final EconomyManager economy;

    public EconomyAdminCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public String name() {
        return "economyadmin";
    }

    @Override
    public List<String> aliases() {
        return List.of("eco", "moneyadmin");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ariatuseconomy.admin")) {
            MessageService.send(sender, "&cNo tienes permisos.");
            return true;
        }

        if (args.length < 4) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        String currencyName = args[2];
        String amountRaw = args[3];

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target.getUniqueId() == null) {
            MessageService.send(sender, "&cNo se pudo encontrar el UUID del jugador.");
            return true;
        }

        Currency currency;

        try {
            currency = Currency.from(currencyName);
        } catch (IllegalArgumentException exception) {
            MessageService.send(sender, "&cMoneda inválida. Usa &eCOINS &co &eODRYS&c.");
            return true;
        }

        BigDecimal amount;

        try {
            amount = new BigDecimal(amountRaw);
        } catch (NumberFormatException exception) {
            MessageService.send(sender, "&cCantidad inválida.");
            return true;
        }

        switch (action) {
            case "give", "add" -> give(sender, target, currency, amount);
            case "take", "remove" -> take(sender, target, currency, amount);
            case "set" -> set(sender, target, currency, amount);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void give(CommandSender sender, OfflinePlayer target, Currency currency, BigDecimal amount) {
        economy.deposit(target.getUniqueId(), currency, amount, "admin_give:" + sender.getName())
                .thenAccept(success -> {
                    if (!success) {
                        MessageService.send(sender, "&cNo se pudo añadir el dinero.");
                        return;
                    }

                    MessageService.send(
                            sender,
                            "&aAñadiste &e" + economy.format(currency, amount)
                                    + " &aa &f" + target.getName() + "&a."
                    );
                });
    }

    private void take(CommandSender sender, OfflinePlayer target, Currency currency, BigDecimal amount) {
        economy.withdraw(target.getUniqueId(), currency, amount, "admin_take:" + sender.getName())
                .thenAccept(success -> {
                    if (!success) {
                        MessageService.send(sender, "&cNo se pudo retirar el dinero. Puede que no tenga suficiente balance.");
                        return;
                    }

                    MessageService.send(
                            sender,
                            "&aRetiraste &e" + economy.format(currency, amount)
                                    + " &ade &f" + target.getName() + "&a."
                    );
                });
    }

    private void set(CommandSender sender, OfflinePlayer target, Currency currency, BigDecimal amount) {
        economy.set(target.getUniqueId(), currency, amount, "admin_set:" + sender.getName())
                .thenAccept(success -> {
                    if (!success) {
                        MessageService.send(sender, "&cNo se pudo establecer el balance.");
                        return;
                    }

                    MessageService.send(
                            sender,
                            "&aEstableciste el balance de &f" + target.getName()
                                    + " &aen &e" + economy.format(currency, amount) + "&a."
                    );
                });
    }

    private void sendHelp(CommandSender sender) {
        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS ECONOMY ADMIN</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&e/economyadmin give <player> <COINS|ODRYS> <amount>");
        MessageService.send(sender, "&e/economyadmin take <player> <COINS|ODRYS> <amount>");
        MessageService.send(sender, "&e/economyadmin set <player> <COINS|ODRYS> <amount>");
        MessageService.send(sender, "");
        MessageService.send(sender, "&7Aliases: &f/eco&7, &f/moneyadmin");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }
}