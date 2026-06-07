package net.ariatus.project.api.chunk;

import java.util.Collection;
import java.util.Optional;

public interface ChunkPreloadService {

    boolean start(String worldId, int radius);

    boolean start(String worldId, int centerX, int centerZ, int radius);

    boolean pause(String worldId);

    boolean resume(String worldId);

    boolean cancel(String worldId);

    Optional<ChunkPreloadTaskView> task(String worldId);

    Collection<ChunkPreloadTaskView> tasks();
}