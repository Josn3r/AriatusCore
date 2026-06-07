package net.ariatus.project.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageRepository {

    CompletableFuture<Boolean> unlocked(UUID uuid, int chestNumber);

    CompletableFuture<Void> unlock(UUID uuid, int chestNumber);

    CompletableFuture<String> loadContent(UUID uuid, int chestNumber);

    CompletableFuture<Void> saveContent(UUID uuid, int chestNumber, String content);
}