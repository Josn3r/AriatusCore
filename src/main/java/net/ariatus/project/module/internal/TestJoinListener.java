package net.ariatus.project.module.internal;

import net.ariatus.project.message.MessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TestJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        MessageService.send(
                event.getPlayer(),
                "<gray>[</gray><gradient:#8A2BE2:#00D4FF>AriatusCore</gradient><gray>]</gray> &7TestModule listener activo."
        );
    }
}