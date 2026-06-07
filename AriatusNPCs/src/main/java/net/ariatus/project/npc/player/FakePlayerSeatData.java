package net.ariatus.project.npc.player;

public class FakePlayerSeatData {

    private final int entityId;
    private final java.util.UUID uuid;

    public FakePlayerSeatData(int entityId, java.util.UUID uuid) {
        this.entityId = entityId;
        this.uuid = uuid;
    }

    public int entityId() {
        return entityId;
    }

    public java.util.UUID uuid() {
        return uuid;
    }
}