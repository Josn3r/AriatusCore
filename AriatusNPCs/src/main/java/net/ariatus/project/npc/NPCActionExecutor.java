package net.ariatus.project.npc;

import net.ariatus.project.AriatusNPCs;
import net.ariatus.project.api.storage.StorageService;
import net.ariatus.project.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class NPCActionExecutor {

    private final AriatusNPCs module;

    public NPCActionExecutor(AriatusNPCs module) {
        this.module = module;
    }

    public void execute(Player player, AriatusNPC npc, NPCAction action) {
        switch (action.type()) {
            case MESSAGE -> MessageService.send(player, format(player, action.value()));
            case CONSOLE_COMMAND -> runConsoleCommand(player, action.value());
            case PLAYER_COMMAND -> runPlayerCommand(player, action.value());
            case PLAYER_COMMAND_AS_OP -> runPlayerCommandAsOp(player, action.value());
            case PLAY_SOUND -> playSound(player, action.value());
            case SEND_TO_SERVER -> sendToServer(player, action.value());
        }
    }

    private void openStorage(Player player) {
        try {
            StorageService storageService = module.services().require(StorageService.class);
            storageService.openMenu(player);
        } catch (Exception exception) {
            MessageService.send(player, "&cEl sistema de baúles no está disponible.");
        }
    }

    private void runPlayerCommand(Player player, String command) {
        if (command == null || command.isBlank()) {
            return;
        }

        player.performCommand(format(player, command));
    }

    private void runConsoleCommand(Player player, String command) {
        if (command == null || command.isBlank()) {
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), format(player, command));
    }

    private void playSound(Player player, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        String[] split = raw.split(",");

        try {
            String soundRaw = split[0].trim();
            float volume = split.length >= 2 ? Float.parseFloat(split[1].trim()) : 1.0f;
            float pitch = split.length >= 3 ? Float.parseFloat(split[2].trim()) : 1.0f;

            org.bukkit.NamespacedKey key = soundKey(soundRaw);
            org.bukkit.Sound sound = org.bukkit.Registry.SOUNDS.get(key);

            if (sound == null) {
                module.logger().warn(module, "Sonido inválido en NPCAction: " + soundRaw);
                return;
            }

            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception exception) {
            module.logger().warn(module, "No se pudo reproducir sonido NPCAction: " + raw);
        }
    }

    private org.bukkit.NamespacedKey soundKey(String raw) {
        String normalized = raw.trim().toLowerCase();

        if (normalized.contains(":")) {
            String[] split = normalized.split(":", 2);
            return new org.bukkit.NamespacedKey(split[0], split[1]);
        }

        normalized = normalized.toLowerCase().replace("_", ".");

        return org.bukkit.NamespacedKey.minecraft(normalized);
    }

    private String format(Player player, String text) {
        return text
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());
    }

    private void runPlayerCommandAsOp(Player player, String command) {
        if (command == null || command.isBlank()) {
            return;
        }

        boolean wasOp = player.isOp();

        try {
            player.setOp(true);
            player.performCommand(format(player, command));
        } finally {
            player.setOp(wasOp);
        }
    }

    private void sendToServer(Player player, String serverName) {
        if (serverName == null || serverName.isBlank()) {
            return;
        }

        try {
            player.sendPluginMessage(module.core(), "BungeeCord", createConnectMessage(serverName));
        } catch (Exception exception) {
            MessageService.send(player, "&cNo se pudo enviarte al servidor &e" + serverName + "&c.");
        }
    }

    private byte[] createConnectMessage(String serverName) throws java.io.IOException {
        java.io.ByteArrayOutputStream byteArray = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream output = new java.io.DataOutputStream(byteArray);

        output.writeUTF("Connect");
        output.writeUTF(serverName);

        return byteArray.toByteArray();
    }
}