package net.ariatus.project.npc.engine;

import net.ariatus.project.npc.AriatusNPC;

public interface NPCSpawnEngine {

    boolean spawn(AriatusNPC npc);

    boolean despawn(AriatusNPC npc);
}