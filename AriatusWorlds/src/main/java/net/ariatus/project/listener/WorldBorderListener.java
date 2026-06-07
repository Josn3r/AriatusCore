package net.ariatus.project.listener;

import net.ariatus.project.message.MessageService;
import net.ariatus.project.world.WorldBorderData;
import net.ariatus.project.world.WorldManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WorldBorderListener implements Listener {

    private final WorldManager worldManager;
    private final Map<UUID, Long> messageCooldown = new HashMap<>();

    public WorldBorderListener(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        Optional<WorldBorderData> optionalBorder =
                worldManager.borderByBukkitWorld(player.getWorld().getName());

        if (optionalBorder.isEmpty()) {
            return;
        }

        WorldBorderData border = optionalBorder.get();

        if (border.inside(event.getTo())) {
            return;
        }

        switch (border.action().toUpperCase()) {
            case "CANCEL" -> event.setCancelled(true);

            case "TELEPORT_BACK" -> {
                event.setCancelled(true);
                player.teleport(border.clamp(event.getFrom()));
            }

            case "PUSH_BACK" -> {
                event.setCancelled(true);
                player.teleport(border.clamp(event.getTo()));
            }

            default -> event.setCancelled(true);
        }

        warn(player, border);
    }

    private void warn(Player player, WorldBorderData border) {
        long now = System.currentTimeMillis();
        long last = messageCooldown.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < 1500) {
            return;
        }

        messageCooldown.put(player.getUniqueId(), now);

        if (border.message() != null && !border.message().isBlank()) {
            MessageService.send(player, border.message());
        }

        if (border.sound() == null || border.sound().isBlank()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(border.sound().toUpperCase());
            player.playSound(player.getLocation(), sound, 0.6f, 0.8f);
        } catch (Exception ignored) {
        }
    }
}