package net.ariatus.project.npc.player;

import net.ariatus.project.npc.AriatusNPC;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FakePlayerRegistry {

    private final Map<String, FakePlayerNPCData> byNpcId = new ConcurrentHashMap<>();
    private final Map<Integer, FakePlayerNPCData> byEntityId = new ConcurrentHashMap<>();
    private final Map<UUID, FakePlayerNPCData> byUuid = new ConcurrentHashMap<>();
    private final Map<String, FakePlayerSeatData> seatsByNpcId = new ConcurrentHashMap<>();

    public FakePlayerNPCData create(AriatusNPC npc) {
        FakePlayerNPCData existing = byNpcId.get(npc.id());

        if (existing != null) {
            return existing;
        }

        UUID uuid = UUID.randomUUID();
        int entityId = ThreadLocalRandom.current().nextInt(200000, Integer.MAX_VALUE);
        String profileName = profileName(npc.id());

        FakePlayerNPCData data = new FakePlayerNPCData(npc, uuid, entityId, profileName);

        data.seat(seat(npc.id()));
        byNpcId.put(npc.id(), data);
        byEntityId.put(entityId, data);
        byUuid.put(uuid, data);

        return data;
    }

    public Optional<FakePlayerNPCData> byNpcId(String npcId) {
        return Optional.ofNullable(byNpcId.get(npcId.toLowerCase()));
    }

    public Optional<FakePlayerNPCData> byEntityId(int entityId) {
        return Optional.ofNullable(byEntityId.get(entityId));
    }

    public Optional<FakePlayerNPCData> byUuid(UUID uuid) {
        return Optional.ofNullable(byUuid.get(uuid));
    }

    public Collection<FakePlayerNPCData> all() {
        return byNpcId.values();
    }

    public void remove(String npcId) {
        FakePlayerNPCData data = byNpcId.remove(npcId.toLowerCase());

        if (data == null) {
            return;
        }

        byEntityId.remove(data.entityId());
        byUuid.remove(data.uuid());
        removeSeat(npcId);
    }

    public void removeByNpcId(String npcId) {
        remove(npcId);
    }

    private String profileName(String id) {
        String clean = id.replaceAll("[^A-Za-z0-9_]", "_");

        if (clean.length() > 16) {
            clean = clean.substring(0, 16);
        }

        if (clean.isBlank()) {
            clean = "AriatusNPC";
        }

        return clean;
    }

    public FakePlayerSeatData seat(String npcId) {
        return seatsByNpcId.computeIfAbsent(npcId.toLowerCase(), ignored ->
                new FakePlayerSeatData(
                        java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 199999),
                        java.util.UUID.randomUUID()
                )
        );
    }

    public java.util.Optional<FakePlayerSeatData> seatIfPresent(String npcId) {
        return java.util.Optional.ofNullable(seatsByNpcId.get(npcId.toLowerCase()));
    }

    public void removeSeat(String npcId) {
        seatsByNpcId.remove(npcId.toLowerCase());
    }
}