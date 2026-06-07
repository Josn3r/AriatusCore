package net.ariatus.project.npc.player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.ariatus.project.AriatusNPCs;
import net.ariatus.project.api.npc.NPCClickType;
import net.ariatus.project.npc.NPCManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerNPCInteractionListener {

    private final AriatusNPCs module;
    private final ProtocolLibHook protocolLibHook;
    private final NPCManager npcManager;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public PlayerNPCInteractionListener(
            AriatusNPCs module,
            ProtocolLibHook protocolLibHook,
            NPCManager npcManager
    ) {
        this.module = module;
        this.protocolLibHook = protocolLibHook;
        this.npcManager = npcManager;
    }

    public void register() {
        if (!protocolLibHook.available()) {
            return;
        }

        protocolLibHook.protocolManager().addPacketListener(new PacketAdapter(
                module.core(),
                PacketType.Play.Client.USE_ENTITY
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handle(event);
            }
        });
    }

    private void handle(PacketEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        int entityId;

        try {
            entityId = event.getPacket().getIntegers().read(0);
        } catch (Exception exception) {
            return;
        }

        npcManager.fakePlayerByEntityId(entityId).ifPresent(data -> {
            if (cooldown(player, data.npc().interactionCooldownMillis())) {
                return;
            }

            NPCClickType clickType = detectClickType(event, player);

            module.core().getServer().getScheduler().runTask(module.core(), () ->
                    npcManager.executeAction(data.npc().id(), clickType, player)
            );
        });
    }

    private NPCClickType detectClickType(PacketEvent event, Player player) {
        String actionName = readUseEntityAction(event);

        boolean attack = actionName.contains("ATTACK");
        boolean interact = actionName.contains("INTERACT");

        if (attack) {
            return player.isSneaking()
                    ? NPCClickType.SHIFT_LEFT_CLICK
                    : NPCClickType.LEFT_CLICK;
        }

        if (interact) {
            return player.isSneaking()
                    ? NPCClickType.SHIFT_RIGHT_CLICK
                    : NPCClickType.RIGHT_CLICK;
        }

        return player.isSneaking()
                ? NPCClickType.SHIFT_RIGHT_CLICK
                : NPCClickType.RIGHT_CLICK;
    }

    private boolean cooldown(Player player, long cooldownMillis) {
        if (cooldownMillis <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        long last = cooldown.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < cooldownMillis) {
            return true;
        }

        cooldown.put(player.getUniqueId(), now);
        return false;
    }

    private String readUseEntityAction(PacketEvent event) {
        try {
            Object modifier = event.getPacket()
                    .getClass()
                    .getMethod("getEnumEntityUseActions")
                    .invoke(event.getPacket());

            Object action = modifier
                    .getClass()
                    .getMethod("read", int.class)
                    .invoke(modifier, 0);

            if (action != null) {
                module.logger().info(module, "USE_ENTITY ACTION: " + action.toString().toUpperCase());
                return action.toString().toUpperCase();
            }
        } catch (Exception ignored) {
        }

        return event.getPacket().toString().toUpperCase();
    }
}