package net.ariatus.project.command;

import net.ariatus.project.AriatusScoreboard;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.service.ScoreboardService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AriatusScoreboardCommand implements AriatusCommandExecutor {

    private final AriatusScoreboard module;
    private final ScoreboardService scoreboardService;

    public AriatusScoreboardCommand(AriatusScoreboard module, ScoreboardService scoreboardService) {
        this.module = module;
        this.scoreboardService = scoreboardService;
    }

    @Override
    public String name() {
        return "ariatusscoreboard";
    }

    @Override
    public List<String> aliases() {
        return List.of("asb", "scoreboard");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("ariatusscoreboard.admin")) {
                    MessageService.send(sender, "&cNo tienes permisos.");
                    return true;
                }

                scoreboardService.reload();
                MessageService.send(sender, "&aAriatusScoreboard recargado correctamente.");
                return true;
            }

            case "toggle" -> {
                if (!(sender instanceof Player player)) {
                    MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
                    return true;
                }

                scoreboardService.toggle(player);

                boolean enabled = scoreboardService.isEnabledFor(player);

                MessageService.send(
                        sender,
                        enabled
                                ? "&aScoreboard activado."
                                : "&cScoreboard desactivado."
                );

                return true;
            }

            case "on", "enable" -> {
                if (!(sender instanceof Player player)) {
                    MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
                    return true;
                }

                scoreboardService.enableFor(player);
                MessageService.send(sender, "&aScoreboard activado.");
                return true;
            }

            case "off", "disable" -> {
                if (!(sender instanceof Player player)) {
                    MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
                    return true;
                }

                scoreboardService.disableFor(player);
                MessageService.send(sender, "&cScoreboard desactivado.");
                return true;
            }

            case "debug" -> {
                if (!sender.hasPermission("ariatusscoreboard.admin")) {
                    MessageService.send(sender, "&cNo tienes permisos.");
                    return true;
                }

                MessageService.send(sender, "");
                MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS SCOREBOARD</bold></gradient>");
                MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
                MessageService.send(sender, "&7Módulo: &f" + module.name());
                MessageService.send(sender, "&7Estado: &a" + module.status());
                MessageService.send(sender, "&7Boards activos: &b" + scoreboardService.activeBoards());
                MessageService.send(sender, "&7Sidebar: " + status(module.configBoolean("config.yml", "sidebar.enabled", true)));
                MessageService.send(sender, "&7TabList: " + status(module.configBoolean("config.yml", "tablist.enabled", true)));
                MessageService.send(sender, "&7BelowName: " + status(module.configBoolean("belowname.yml", "belowname.enabled", true)));
                MessageService.send(sender, "&7Nametag update: &e" + module.configInt("nametags.yml", "settings.update-ticks", 20) + " ticks");
                MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
                MessageService.send(sender, "");

                return true;
            }

            case "help" -> {
                sendHelp(sender);
                return true;
            }

            default -> {
                MessageService.send(sender, "&cSubcomando desconocido. Usa &e/ariatusscoreboard help&c.");
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS SCOREBOARD</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&e/ariatusscoreboard reload &7- Recarga configs.");
        MessageService.send(sender, "&e/ariatusscoreboard toggle &7- Activa/desactiva tu scoreboard.");
        MessageService.send(sender, "&e/ariatusscoreboard on &7- Activa tu scoreboard.");
        MessageService.send(sender, "&e/ariatusscoreboard off &7- Desactiva tu scoreboard.");
        MessageService.send(sender, "&e/ariatusscoreboard debug &7- Muestra estado interno.");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private String status(boolean enabled) {
        return enabled ? "&aActivado" : "&cDesactivado";
    }
}