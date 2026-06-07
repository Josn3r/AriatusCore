package net.ariatus.project.npc.animation;

import net.ariatus.project.api.npc.NPCEngineType;
import net.ariatus.project.npc.AriatusNPC;
import net.ariatus.project.npc.NPCManager;
import net.ariatus.project.npc.player.ProtocolLibHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class NPCAnimationService {

    private final NPCManager npcManager;
    private final ProtocolLibHook protocolLibHook;

    public NPCAnimationService(NPCManager npcManager, ProtocolLibHook protocolLibHook) {
        this.npcManager = npcManager;
        this.protocolLibHook = protocolLibHook;
    }

    public boolean play(String npcId, NPCAnimationType type) {
        var optionalNPC = npcManager.internalNPC(npcId);

        if (optionalNPC.isEmpty()) {
            return false;
        }

        AriatusNPC npc = optionalNPC.get();

        if (npc.engineType() == NPCEngineType.PLAYER) {
            return playFakePlayer(npc, type);
        }

        return playEntity(npc, type);
    }

    private boolean playEntity(AriatusNPC npc, NPCAnimationType type) {
        Entity entity = npc.entity();

        if (entity == null || !entity.isValid()) {
            return false;
        }

        switch (type) {
            case SWING_MAIN_HAND -> {
                if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
                    livingEntity.swingMainHand();
                    return true;
                }
            }
            case SWING_OFF_HAND -> {
                if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
                    livingEntity.swingOffHand();
                    return true;
                }
            }
            case HURT -> {
                entity.getWorld().playEffect(entity.getLocation(), org.bukkit.Effect.STEP_SOUND, org.bukkit.Material.REDSTONE_BLOCK);
                return true;
            }
            default -> {
                return false;
            }
        }

        return false;
    }

    private boolean playFakePlayer(AriatusNPC npc, NPCAnimationType type) {
        if (!protocolLibHook.available()) {
            return false;
        }

        var optionalData = npcManager.fakePlayerByNpcId(npc.id());

        if (optionalData.isEmpty()) {
            return false;
        }

        var data = optionalData.get();

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getWorld().equals(npc.location().getWorld())) {
                continue;
            }

            protocolLibHook.packetService().sendAnimation(viewer, data, type);
        }

        return true;
    }
}