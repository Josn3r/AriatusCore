package net.ariatus.project.npc;

import net.ariatus.project.api.npc.NPCClickType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCListener implements Listener {

    private final NPCManager npcManager;
    private final Map<UUID, Long> clickCooldown = new HashMap<>();

    public NPCListener(NPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        npcManager.byEntity(event.getRightClicked()).ifPresent(npc -> {
            event.setCancelled(true);
            handle(event.getPlayer(), event.getRightClicked(), true);
        });
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        npcManager.byEntity(event.getRightClicked()).ifPresent(npc -> {
            event.setCancelled(true);
            handle(event.getPlayer(), event.getRightClicked(), true);
        });
    }

    @EventHandler(ignoreCancelled = false)
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        npcManager.byEntity(event.getEntity()).ifPresent(npc -> {
            event.setCancelled(true);

            if (cooldown(player, npc.interactionCooldownMillis())) {
                return;
            }

            NPCClickType clickType = player.isSneaking()
                    ? NPCClickType.SHIFT_LEFT_CLICK
                    : NPCClickType.LEFT_CLICK;

            npcManager.executeAction(npc.id(), clickType, player);
        });
    }

    private void handle(Player player, Entity entity, boolean rightClick) {
        npcManager.byEntity(entity).ifPresent(npc -> {
            if (cooldown(player, npc.interactionCooldownMillis())) {
                return;
            }

            NPCClickType clickType;

            if (rightClick) {
                clickType = player.isSneaking()
                        ? NPCClickType.SHIFT_RIGHT_CLICK
                        : NPCClickType.RIGHT_CLICK;
            } else {
                clickType = player.isSneaking()
                        ? NPCClickType.SHIFT_LEFT_CLICK
                        : NPCClickType.LEFT_CLICK;
            }

            npcManager.executeAction(npc.id(), clickType, player);
        });
    }

    private boolean cooldown(Player player, long cooldownMillis) {
        if (cooldownMillis <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        long last = clickCooldown.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < cooldownMillis) {
            return true;
        }

        clickCooldown.put(player.getUniqueId(), now);
        return false;
    }
}