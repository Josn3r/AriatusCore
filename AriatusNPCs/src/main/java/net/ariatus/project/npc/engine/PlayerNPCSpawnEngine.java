package net.ariatus.project.npc.engine;

import net.ariatus.project.AriatusNPCs;
import net.ariatus.project.npc.AriatusNPC;
import net.ariatus.project.npc.player.FakePlayerNPCData;
import net.ariatus.project.npc.player.FakePlayerRegistry;
import net.ariatus.project.npc.player.ProtocolLibHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerNPCSpawnEngine implements NPCSpawnEngine {

    private final AriatusNPCs module;
    private final ProtocolLibHook protocolLibHook;
    private final FakePlayerRegistry registry;

    public PlayerNPCSpawnEngine(
            AriatusNPCs module,
            ProtocolLibHook protocolLibHook,
            FakePlayerRegistry registry
    ) {
        this.module = module;
        this.protocolLibHook = protocolLibHook;
        this.registry = registry;
    }

    @Override
    public boolean spawn(AriatusNPC npc) {
        if (!protocolLibHook.available()) {
            module.logger().warn(module, "No se puede spawnear PLAYER NPC sin ProtocolLib: " + npc.id());
            return false;
        }

        if (npc.location() == null || npc.location().getWorld() == null) {
            return false;
        }

        FakePlayerNPCData data = registry.create(npc);
        npc.entityUuid(data.uuid());

        try {
            var scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            scoreboard.resetScores(data.profileName());
            scoreboard.resetScores(npc.id());
        } catch (Exception ignored) {
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getWorld().equals(npc.location().getWorld())) {
                continue;
            }

            protocolLibHook.packetService().spawn(viewer, data);
        }

        module.logger().info(module, "PLAYER NPC spawneado: " + npc.id());
        module.logger().info(module,
                "Spawn PLAYER NPC: id=" + npc.id()
                        + ", profile=" + data.profileName()
                        + ", uuid=" + data.uuid()
                        + ", entityId=" + data.entityId()
                        + ", world=" + npc.location().getWorld().getName()
                        + ", x=" + npc.location().getX()
                        + ", y=" + npc.location().getY()
                        + ", z=" + npc.location().getZ()
        );
        return true;
    }

    @Override
    public boolean despawn(AriatusNPC npc) {
        if (!protocolLibHook.available()) {
            npc.entityUuid(null);
            registry.remove(npc.id());
            return true;
        }

        registry.byNpcId(npc.id()).ifPresent(data -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                protocolLibHook.packetService().destroy(viewer, data);
            }
        });

        npc.entityUuid(null);
        registry.remove(npc.id());
        return true;
    }
}