package net.ariatus.project.commands;

import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.profile.Profile;
import net.ariatus.project.profile.ProfileManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ProfileCommand implements AriatusCommandExecutor {

    private final ProfileManager profileManager;

    public ProfileCommand(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public List<String> aliases() {
        return List.of("perfil", "stats");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return true;
        }

        profileManager.updatePlaytime(player.getUniqueId());

        Profile profile = profileManager.fullProfile(player).orElse(null);

        if (profile == null) {
            MessageService.send(sender, "&cTu perfil todavía está cargando. Intenta nuevamente.");
            return true;
        }

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS PROFILE</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7Jugador: &f" + profile.name());
        MessageService.send(sender, "&7Nivel: &e" + profile.level());
        MessageService.send(sender, "&7Experiencia: &b" + profile.experience());
        MessageService.send(sender, "&7Experiencia total: &b" + profile.totalExperience());
        MessageService.send(sender, "&7Reputación: &a" + profile.reputation());
        MessageService.send(sender, "&7Tiempo jugado: &d" + formatPlaytime(profile.playtimeSeconds()));
        MessageService.send(sender, "");
        MessageService.send(sender, "&7Kills: &c" + profile.kills());
        MessageService.send(sender, "&7Muertes: &c" + profile.deaths());
        MessageService.send(sender, "&7Bloques rotos: &e" + profile.blocksBroken());
        MessageService.send(sender, "&7Bloques puestos: &e" + profile.blocksPlaced());
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");

        return true;
    }

    private String formatPlaytime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + secs + "s";
        }

        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }

        return secs + "s";
    }
}