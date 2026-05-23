package net.ariatus.project.api.profile;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public interface ProfileService {

    Optional<ProfileView> profile(UUID uuid);

    default Optional<ProfileView> profile(Player player) {
        return profile(player.getUniqueId());
    }

    boolean loaded(UUID uuid);

    default boolean loaded(Player player) {
        return loaded(player.getUniqueId());
    }
}