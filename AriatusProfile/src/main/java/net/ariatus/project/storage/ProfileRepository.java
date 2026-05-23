package net.ariatus.project.storage;

import net.ariatus.project.profile.Profile;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProfileRepository {

    CompletableFuture<Profile> loadOrCreate(Player player);

    CompletableFuture<Void> save(Profile profile);

    CompletableFuture<Boolean> exists(UUID uuid);
}