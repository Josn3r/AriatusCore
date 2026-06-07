package net.ariatus.project.api.storage;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageService {

    void openMenu(Player player);

    void openChest(Player player, int chestNumber);

    CompletableFuture<Boolean> unlockChest(UUID uuid, int chestNumber);

    CompletableFuture<Boolean> unlocked(UUID uuid, int chestNumber);

    Optional<StorageChestView> chest(UUID uuid, int chestNumber);
}