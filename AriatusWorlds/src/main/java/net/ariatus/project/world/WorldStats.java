package net.ariatus.project.world;

public record WorldStats(
        String worldName,
        int loadedChunks,
        int players,
        int entities,
        int livingEntities,
        int mobs,
        int animals,
        int monsters,
        int droppedItems,
        int armorStands,
        int villagers,
        int tileEntities,
        double borderSize,
        boolean pvp,
        String difficulty,
        long fullTime,
        boolean storm,
        boolean thundering
) {
}