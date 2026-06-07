package net.ariatus.project.api.world;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface WorldService {

    Collection<WorldView> worlds();

    Optional<WorldView> world(String id);

    boolean exists(String id);

    boolean loaded(String id);

    CompletableFuture<WorldLoadResult> load(String id);

    CompletableFuture<Boolean> unload(String id, boolean save);

    CompletableFuture<Boolean> delete(String id);

    CompletableFuture<WorldTeleportResult> teleport(Player player, String id);

    CompletableFuture<WorldTeleportResult> teleport(Player player, String id, String spawnName);

    Optional<Location> spawn(String id);

    Optional<Location> spawn(String id, String spawnName);
}