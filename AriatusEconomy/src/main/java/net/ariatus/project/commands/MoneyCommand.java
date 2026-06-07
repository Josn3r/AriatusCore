package net.ariatus.project.commands;

import net.ariatus.project.api.economy.Currency;
import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.economy.EconomyManager;
import net.ariatus.project.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class MoneyCommand implements AriatusCommandExecutor {

    private final EconomyManager economy;

    public MoneyCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public String name() {
        return "money";
    }

    @Override
    public List<String> aliases() {
        return List.of("balance", "bal", "dinero");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                MessageService.send(sender, "&cUso: /money <jugador>");
                return true;
            }

            showBalance(sender, player.getUniqueId(), player.getName());
            return true;
        }

        if (!sender.hasPermission("ariatuseconomy.money.others")) {
            MessageService.send(sender, "&cNo tienes permisos para ver el dinero de otros jugadores.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            MessageService.send(sender, "&cJugador no encontrado o no está conectado.");
            return true;
        }

        showBalance(sender, target.getUniqueId(), target.getName());
        return true;
    }

    private void showBalance(CommandSender sender, UUID uuid, String name) {
        economy.balance(uuid, Currency.COINS).thenCombine(
                economy.balance(uuid, Currency.ODRYS),
                (coins, odrys) -> {
                    MessageService.send(sender, "");
                    MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS ECONOMY</bold></gradient>");
                    MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
                    MessageService.send(sender, "&7Jugador: &f" + name);
                    MessageService.send(sender, "&7Moneda principal: &e" + economy.format(Currency.COINS, coins));
                    MessageService.send(sender, "&7Odrys: &b" + economy.format(Currency.ODRYS, odrys));
                    MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
                    MessageService.send(sender, "");
                    return null;
                }
        );
    }
}