package net.ariatus.project.api.chunk;

public interface ChunkPreloadTaskView {

    String worldId();

    String worldName();

    int centerX();

    int centerZ();

    int radius();

    int currentChunkX();

    int currentChunkZ();

    long processedChunks();

    long totalChunks();

    ChunkPreloadState state();

    default boolean running() {
        return state() == ChunkPreloadState.RUNNING;
    }

    default boolean paused() {
        return state() == ChunkPreloadState.PAUSED;
    }

    default boolean completed() {
        return state() == ChunkPreloadState.COMPLETED;
    }

    default boolean cancelled() {
        return state() == ChunkPreloadState.CANCELLED;
    }

    default boolean failed() {
        return state() == ChunkPreloadState.FAILED;
    }

    default double progress() {
        if (totalChunks() <= 0) {
            return 0.0;
        }

        return Math.min(100.0, (processedChunks() * 100.0) / totalChunks());
    }
}