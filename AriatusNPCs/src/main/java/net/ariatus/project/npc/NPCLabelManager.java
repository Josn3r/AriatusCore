package net.ariatus.project.npc;

import net.ariatus.project.api.npc.NPCEngineType;
import net.ariatus.project.message.MessageService;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCLabelManager {

    private final Map<String, UUID> labels = new HashMap<>();

    public void spawnOrUpdate(AriatusNPC npc) {
        if (npc.engineType() != NPCEngineType.PLAYER) {
            return;
        }

        despawn(npc.id());

        if (!npc.nameVisible() || npc.displayName() == null || npc.displayName().isBlank()) {
            return;
        }

        if (npc.location() == null || npc.location().getWorld() == null) {
            return;
        }

        Location location = npc.location().clone().add(0.0, labelHeight(npc), 0.0);

        TextDisplay display = npc.location().getWorld().spawn(location, TextDisplay.class, textDisplay -> {
            textDisplay.text(MessageService.parse(npc.displayName()));
            textDisplay.setBillboard(Display.Billboard.CENTER);
            textDisplay.setSeeThrough(false);
            textDisplay.setShadowed(true);
            textDisplay.setPersistent(false);
            textDisplay.setGravity(false);
            textDisplay.setInvulnerable(true);
            textDisplay.setDefaultBackground(false);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            textDisplay.addScoreboardTag("ariatus_npc_label");
            textDisplay.addScoreboardTag("ariatus_npc_label_" + npc.id());
        });

        labels.put(npc.id().toLowerCase(), display.getUniqueId());
    }

    public void despawn(String npcId) {
        UUID uuid = labels.remove(npcId.toLowerCase());

        if (uuid == null) {
            return;
        }

        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            Entity entity = world.getEntity(uuid);

            if (entity != null) {
                entity.remove();
                return;
            }
        }
    }

    public void updateLocation(AriatusNPC npc) {
        UUID uuid = labels.get(npc.id().toLowerCase());

        if (uuid == null || npc.location() == null || npc.location().getWorld() == null) {
            return;
        }

        Entity entity = npc.location().getWorld().getEntity(uuid);

        if (entity == null) {
            spawnOrUpdate(npc);
            return;
        }

        entity.teleport(npc.location().clone().add(0.0, labelHeight(npc), 0.0));
    }

    public void despawnAll() {
        for (String id : new java.util.ArrayList<>(labels.keySet())) {
            despawn(id);
        }
    }

    private double labelHeight(AriatusNPC npc) {
        double scale = Math.max(0.1, npc.size());
        return 2.25 * scale;
    }
}