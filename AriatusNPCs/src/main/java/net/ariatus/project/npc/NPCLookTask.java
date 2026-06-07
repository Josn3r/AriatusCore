package net.ariatus.project.npc;

import net.ariatus.project.api.npc.NPCEngineType;
import net.ariatus.project.npc.player.ProtocolLibHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class NPCLookTask implements Runnable {

    private final NPCManager npcManager;
    private final ProtocolLibHook protocolLibHook;

    public NPCLookTask(NPCManager npcManager, ProtocolLibHook protocolLibHook) {
        this.npcManager = npcManager;
        this.protocolLibHook = protocolLibHook;
    }

    @Override
    public void run() {
        for (AriatusNPC npc : npcManager.internalNPCs()) {
            if (!npc.spawned()) {
                continue;
            }

            if (!npc.turnToPlayer()) {
                continue;
            }

            if (npc.location() == null || npc.location().getWorld() == null) {
                continue;
            }

            Player target = nearestPlayer(npc);

            if (target == null) {
                continue;
            }

            Location lookedLocation = lookAt(
                    npc.location().clone(),
                    npcEyeLocation(npc),
                    targetHeadLocation(target)
            );

            npc.location(lookedLocation);

            if (npc.engineType() == NPCEngineType.PLAYER) {
                updateFakePlayerLook(npc);
            } else {
                updateEntityLook(npc, lookedLocation);
            }

            npcManager.labelManager().updateLocation(npc);
        }
    }

    private Player nearestPlayer(AriatusNPC npc) {
        Location location = npc.location();

        if (location == null || location.getWorld() == null) {
            return null;
        }

        double maxDistance = npc.turnToPlayerDistance();

        if (maxDistance <= 0) {
            return null;
        }

        double maxDistanceSquared = maxDistance * maxDistance;

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : location.getWorld().getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            double distance = player.getLocation().distanceSquared(location);

            if (distance > maxDistanceSquared) {
                continue;
            }

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    private Location lookAt(Location baseLocation, Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distanceXZ));

        // Suaviza un poco el pitch para que no mire exageradamente arriba/abajo.
        pitch = clamp(pitch, -35.0f, 35.0f);

        baseLocation.setYaw(yaw);
        baseLocation.setPitch(pitch);

        return baseLocation;
    }

    private void updateEntityLook(AriatusNPC npc, Location location) {
        Entity entity = npc.entity();

        if (entity == null || !entity.isValid()) {
            return;
        }

        entity.teleport(location);
    }

    private void updateFakePlayerLook(AriatusNPC npc) {
        if (!protocolLibHook.available()) {
            return;
        }

        npcManager.fakePlayerByNpcId(npc.id()).ifPresent(data -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.getWorld().equals(npc.location().getWorld())) {
                    continue;
                }

                protocolLibHook.packetService().sendLook(viewer, data);
            }
        });
    }

    private Location npcEyeLocation(AriatusNPC npc) {
        Location location = npc.location().clone();

        double eyeHeight;

        if (npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            eyeHeight = 1.62 * Math.max(0.1, npc.size());
        } else {
            Entity entity = npc.entity();

            if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
                eyeHeight = livingEntity.getEyeHeight();
            } else {
                eyeHeight = 1.62 * Math.max(0.1, npc.size());
            }
        }

        return location.add(0.0, eyeHeight, 0.0);
    }

    private Location targetHeadLocation(Player player) {
        // Apunta a los ojos/cabeza del jugador más cercano.
        return player.getEyeLocation().clone().subtract(0.0, 0.15, 0.0);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}