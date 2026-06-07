package net.ariatus.project.chunk;

import net.ariatus.project.api.chunk.ChunkPreloadState;
import net.ariatus.project.api.chunk.ChunkPreloadTaskView;

public class ChunkPreloadTask implements ChunkPreloadTaskView {

    private final String worldId;
    private final String worldName;
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int minChunkX;
    private final int maxChunkX;
    private final int minChunkZ;
    private final int maxChunkZ;
    private final long totalChunks;

    private int currentChunkX;
    private int currentChunkZ;
    private long processedChunks;
    private ChunkPreloadState state;

    public ChunkPreloadTask(
            String worldId,
            String worldName,
            int centerX,
            int centerZ,
            int radius,
            int currentChunkX,
            int currentChunkZ,
            long processedChunks,
            ChunkPreloadState state
    ) {
        this.worldId = worldId;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;

        this.minChunkX = Math.floorDiv(centerX - radius, 16);
        this.maxChunkX = Math.floorDiv(centerX + radius, 16);
        this.minChunkZ = Math.floorDiv(centerZ - radius, 16);
        this.maxChunkZ = Math.floorDiv(centerZ + radius, 16);

        this.currentChunkX = currentChunkX;
        this.currentChunkZ = currentChunkZ;
        this.processedChunks = processedChunks;
        this.state = state;

        long width = (long) maxChunkX - minChunkX + 1L;
        long height = (long) maxChunkZ - minChunkZ + 1L;
        this.totalChunks = width * height;
    }

    public static ChunkPreloadTask fresh(
            String worldId,
            String worldName,
            int centerX,
            int centerZ,
            int radius
    ) {
        int minChunkX = Math.floorDiv(centerX - radius, 16);
        int minChunkZ = Math.floorDiv(centerZ - radius, 16);

        return new ChunkPreloadTask(
                worldId,
                worldName,
                centerX,
                centerZ,
                radius,
                minChunkX,
                minChunkZ,
                0,
                ChunkPreloadState.RUNNING
        );
    }

    @Override
    public String worldId() {
        return worldId;
    }

    @Override
    public String worldName() {
        return worldName;
    }

    @Override
    public int centerX() {
        return centerX;
    }

    @Override
    public int centerZ() {
        return centerZ;
    }

    @Override
    public int radius() {
        return radius;
    }

    public int minChunkX() {
        return minChunkX;
    }

    public int maxChunkX() {
        return maxChunkX;
    }

    public int minChunkZ() {
        return minChunkZ;
    }

    public int maxChunkZ() {
        return maxChunkZ;
    }

    @Override
    public int currentChunkX() {
        return currentChunkX;
    }

    @Override
    public int currentChunkZ() {
        return currentChunkZ;
    }

    @Override
    public long processedChunks() {
        return processedChunks;
    }

    @Override
    public long totalChunks() {
        return totalChunks;
    }

    @Override
    public boolean completed() {
        return state == ChunkPreloadState.COMPLETED;
    }

    @Override
    public ChunkPreloadState state() {
        return state;
    }

    public void state(ChunkPreloadState state) {
        this.state = state;
    }

    public void advance() {
        processedChunks++;

        currentChunkX++;

        if (currentChunkX > maxChunkX) {
            currentChunkX = minChunkX;
            currentChunkZ++;
        }

        if (currentChunkZ > maxChunkZ) {
            state = ChunkPreloadState.COMPLETED;
        }
    }
}