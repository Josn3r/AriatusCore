package net.ariatus.project.api.npc;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public interface NPCView {

    String id();

    String displayName();

    NPCEngineType engineType();

    EntityType entityType();

    Location location();

    boolean spawned();

    UUID entityUuid();
}