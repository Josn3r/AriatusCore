package net.ariatus.project.api.npc;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public interface NPCService {

    Collection<NPCView> npcs();

    Optional<NPCView> npc(String id);

    boolean spawn(String id);

    boolean despawn(String id);

    boolean createEntity(String id, EntityType type, Location location) throws IOException;

    boolean createPlayer(String id, String skinName, Location location);

    boolean delete(String id) throws IOException;

    boolean executeAction(String id, NPCClickType clickType, Player player);

    boolean isNPCScoreboardEntry(String entry);
}