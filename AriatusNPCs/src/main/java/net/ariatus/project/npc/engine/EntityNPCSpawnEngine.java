package net.ariatus.project.npc.engine;

import net.ariatus.project.AriatusNPCs;
import net.ariatus.project.npc.AriatusNPC;
import net.ariatus.project.npc.NPCPropertyApplier;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class EntityNPCSpawnEngine implements NPCSpawnEngine {

    private final AriatusNPCs module;
    private final NPCPropertyApplier propertyApplier;

    public EntityNPCSpawnEngine(AriatusNPCs module, NPCPropertyApplier propertyApplier) {
        this.module = module;
        this.propertyApplier = propertyApplier;
    }

    @Override
    public boolean spawn(AriatusNPC npc) {
        if (npc.location() == null || npc.location().getWorld() == null) {
            return false;
        }

        Entity existing = npc.entity();

        if (existing != null && existing.isValid()) {
            return true;
        }

        try {
            Location location = npc.location();

            Entity entity = location.getWorld().spawnEntity(location, npc.entityType());

            entity.addScoreboardTag("ariatus_npc");
            entity.addScoreboardTag("ariatus_npc_" + npc.id());

            propertyApplier.apply(npc, entity);

            npc.entityUuid(entity.getUniqueId());

            module.logger().info(module, "NPC spawneado: " + npc.id() + " (" + npc.entityType().name() + ")");
            return true;
        } catch (Exception exception) {
            module.logger().error(module, "Error spawneando NPC " + npc.id() + ": " + exception.getMessage());
            return false;
        }
    }

    @Override
    public boolean despawn(AriatusNPC npc) {
        Entity entity = npc.entity();

        if (entity == null) {
            npc.entityUuid(null);
            return true;
        }

        entity.remove();
        npc.entityUuid(null);
        return true;
    }
}