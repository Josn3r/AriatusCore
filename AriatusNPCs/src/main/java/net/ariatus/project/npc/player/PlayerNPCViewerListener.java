package net.ariatus.project.npc.player;

import net.ariatus.project.AriatusNPCs;
import net.ariatus.project.npc.NPCManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerNPCViewerListener implements Listener {

    private final AriatusNPCs module;
    private final ProtocolLibHook protocolLibHook;
    private final NPCManager npcManager;

    public PlayerNPCViewerListener(AriatusNPCs module, ProtocolLibHook protocolLibHook, NPCManager npcManager) {
        this.module = module;
        this.protocolLibHook = protocolLibHook;
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!protocolLibHook.available()) {
            return;
        }

        module.core().getServer().getScheduler().runTaskLater(module.core(), () ->
                npcManager.fakePlayers().forEach(data -> {
                    if (event.getPlayer().getWorld().equals(data.npc().location().getWorld())) {
                        protocolLibHook.packetService().spawn(event.getPlayer(), data);
                    }
                }), 20L
        );
    }
}