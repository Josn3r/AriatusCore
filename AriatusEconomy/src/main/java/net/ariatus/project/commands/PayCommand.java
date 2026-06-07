package net.ariatus.project.commands;

import net.ariatus.project.api.economy.Currency;
import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.economy.EconomyManager;
import net.ariatus.project.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;

public class PayCommand implements AriatusCommandExecutor {

    private final EconomyManager economy;

    public PayCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public String name() {
        return "pay";
    }

    @Override
    public List<String> aliases() {
        return List.of("pagar");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return true;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/pay <jugador> <cantidad>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            MessageService.send(sender, "&cJugador no encontrado o no está conectado.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            MessageService.send(sender, "&cNo puedes pagarte a ti mismo.");
            return true;
        }

        BigDecimal amount;

        try {
            amount = new BigDecimal(args[1]);
        } catch (NumberFormatException exception) {
            MessageService.send(sender, "&cCantidad inválida.");
            return true;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            MessageService.send(sender, "&cLa cantidad debe ser mayor que 0.");
            return true;
        }

        economy.transfer(
                player.getUniqueId(),
                target.getUniqueId(),
                Currency.COINS,
                amount,
                "player_pay"
        ).thenAccept(success -> {
            if (!success) {
                MessageService.send(player, "&cNo tienes suficiente dinero o la transferencia falló.");
                return;
            }

            String formatted = economy.format(Currency.COINS, amount);

            MessageService.send(player, "&aEnviaste &e" + formatted + " &aa &f" + target.getName() + "&a.");
            MessageService.send(target, "&aRecibiste &e" + formatted + " &ade &f" + player.getName() + "&a.");
        });

        return true;
    }
}