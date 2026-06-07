package net.ariatus.project.npc.player;

import net.ariatus.project.npc.AriatusNPC;

import java.util.UUID;

public class FakePlayerNPCData {

    private final AriatusNPC npc;
    private final UUID uuid;
    private final int entityId;
    private final String profileName;

    public FakePlayerNPCData(AriatusNPC npc, UUID uuid, int entityId, String profileName) {
        this.npc = npc;
        this.uuid = uuid;
        this.entityId = entityId;
        this.profileName = profileName;
    }

    public AriatusNPC npc() {
        return npc;
    }

    public UUID uuid() {
        return uuid;
    }

    public int entityId() {
        return entityId;
    }

    public String profileName() {
        return profileName;
    }

    private FakePlayerSeatData seat;

    public FakePlayerSeatData seat() {
        return seat;
    }

    public void seat(FakePlayerSeatData seat) {
        this.seat = seat;
    }
}