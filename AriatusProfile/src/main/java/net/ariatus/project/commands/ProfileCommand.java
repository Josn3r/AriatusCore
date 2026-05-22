package net.ariatus.project.commands;

import net.ariatus.project.command.AriatusCommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ProfileCommand implements AriatusCommandExecutor {
    @Override
    public String name() {
        return "profile";
    }

    @Override
    public List<String> aliases() {
        return List.of("perfil", "prof");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        player.sendMessage("§aPerfil abierto correctamente.");
        return true;
    }
}
