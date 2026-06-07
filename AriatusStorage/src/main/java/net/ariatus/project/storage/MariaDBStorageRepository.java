package net.ariatus.project.storage;

import net.ariatus.project.AriatusStorage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MariaDBStorageRepository implements StorageRepository {

    private final AriatusStorage module;

    public MariaDBStorageRepository(AriatusStorage module) {
        this.module = module;
    }

    @Override
    public CompletableFuture<Boolean> unlocked(UUID uuid, int chestNumber) {
        if (chestNumber == 1) {
            return CompletableFuture.completedFuture(true);
        }

        return module.database().queryAsync(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT unlocked
                    FROM ariatus_storage_chests
                    WHERE uuid = ? AND chest_number = ?
                    """)) {

                statement.setString(1, uuid.toString());
                statement.setInt(2, chestNumber);

                try (var result = statement.executeQuery()) {
                    if (result.next()) {
                        return result.getBoolean("unlocked");
                    }
                }
            }

            return false;
        }, false);
    }

    @Override
    public CompletableFuture<Void> unlock(UUID uuid, int chestNumber) {
        return module.database().updateAsync(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO ariatus_storage_chests
                    (uuid, chest_number, unlocked)
                    VALUES (?, ?, true)
                    ON DUPLICATE KEY UPDATE unlocked = true
                    """)) {

                statement.setString(1, uuid.toString());
                statement.setInt(2, chestNumber);

                return statement.executeUpdate();
            }
        }).thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<String> loadContent(UUID uuid, int chestNumber) {
        return module.database().queryAsync(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT content
                    FROM ariatus_storage_contents
                    WHERE uuid = ? AND chest_number = ?
                    """)) {

                statement.setString(1, uuid.toString());
                statement.setInt(2, chestNumber);

                try (var result = statement.executeQuery()) {
                    if (result.next()) {
                        return result.getString("content");
                    }
                }
            }

            return "";
        }, "");
    }

    @Override
    public CompletableFuture<Void> saveContent(UUID uuid, int chestNumber, String content) {
        return module.database().updateAsync(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO ariatus_storage_contents
                    (uuid, chest_number, content)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE content = VALUES(content)
                    """)) {

                statement.setString(1, uuid.toString());
                statement.setInt(2, chestNumber);
                statement.setString(3, content);

                return statement.executeUpdate();
            }
        }).thenApply(ignored -> null);
    }
}