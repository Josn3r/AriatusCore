package net.ariatus.project.commands;

import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.profile.Profile;
import net.ariatus.project.profile.ProfileManager;
import net.ariatus.project.service.ExperienceService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ProfileAdminCommand implements AriatusCommandExecutor {

    private final ProfileManager profileManager;
    private final ExperienceService experienceService;

    public ProfileAdminCommand(ProfileManager profileManager, ExperienceService experienceService) {
        this.profileManager = profileManager;
        this.experienceService = experienceService;
    }

    @Override
    public String name() {
        return "profileadmin";
    }

    @Override
    public List<String> aliases() {
        return List.of("pa");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ariatusprofile.admin")) {
            MessageService.send(sender, "&cNo tienes permisos.");
            return true;
        }

        if (args.length < 4) {
            sendHelp(sender);
            return true;
        }

        String category = args[0].toLowerCase();
        String action = args[1].toLowerCase();
        String targetName = args[2];

        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            MessageService.send(sender, "&cJugador no encontrado o no está conectado.");
            return true;
        }

        Profile profile = profileManager.fullProfile(target).orElse(null);

        if (profile == null) {
            MessageService.send(sender, "&cEl perfil del jugador todavía no está cargado.");
            return true;
        }

        switch (category) {
            case "xp", "experience" -> handleExperience(sender, target, profile, action, args);
            case "level" -> handleLevel(sender, target, profile, action, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleExperience(CommandSender sender, Player target, Profile profile, String action, String[] args) {
        long amount = parseLong(args[3], -1);

        if (amount < 0) {
            MessageService.send(sender, "&cCantidad inválida.");
            return;
        }

        switch (action) {
            case "add" -> {
                experienceService.addExperience(target, profile, amount);
                MessageService.send(sender, "&aAñadiste &e" + amount + " XP&a a &f" + target.getName() + "&a.");
            }

            case "set" -> {
                experienceService.setExperience(target, profile, amount);
                MessageService.send(sender, "&aEstableciste la XP de &f" + target.getName() + " &aen &e" + amount + "&a.");
            }

            default -> sendHelp(sender);
        }
    }

    private void handleLevel(CommandSender sender, Player target, Profile profile, String action, String[] args) {
        int level = parseInt(args[3], -1);

        if (level <= 0) {
            MessageService.send(sender, "&cNivel inválido.");
            return;
        }

        if (action.equalsIgnoreCase("set")) {
            experienceService.setLevel(target, profile, level);
            MessageService.send(sender, "&aEstableciste el nivel de &f" + target.getName() + " &aen &e" + level + "&a.");
            return;
        }

        sendHelp(sender);
    }

    private void sendHelp(CommandSender sender) {
        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS PROFILE ADMIN</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&e/profileadmin xp add <player> <amount>");
        MessageService.send(sender, "&e/profileadmin xp set <player> <amount>");
        MessageService.send(sender, "&e/profileadmin level set <player> <level>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private long parseLong(String value, long def) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    private int parseInt(String value, int def) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return def;
        }
    }
}