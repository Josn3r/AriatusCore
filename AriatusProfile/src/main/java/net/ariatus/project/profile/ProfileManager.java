package net.ariatus.project.profile;

import net.ariatus.project.AriatusProfile;
import net.ariatus.project.api.profile.ProfileService;
import net.ariatus.project.api.profile.ProfileView;
import net.ariatus.project.storage.ProfileRepository;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManager implements ProfileService {

    private final AriatusProfile module;
    private final ProfileRepository repository;

    private final Map<UUID, Profile> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> sessionStart = new ConcurrentHashMap<>();

    public ProfileManager(AriatusProfile module, ProfileRepository repository) {
        this.module = module;
        this.repository = repository;
    }

    public void loadProfile(Player player) {
        repository.loadOrCreate(player).thenAccept(profile -> {
            cache.put(player.getUniqueId(), profile);
            sessionStart.put(player.getUniqueId(), Instant.now());
            module.logger().info(module, "Perfil cargado: " + player.getName());
        });
    }

    public void saveProfile(UUID uuid) {
        Profile profile = cache.get(uuid);

        if (profile == null) {
            return;
        }

        updatePlaytime(uuid);

        repository.save(profile).thenRun(() ->
                module.logger().debug(module, "Perfil guardado: " + profile.name())
        );
    }

    public void unloadProfile(Player player) {
        UUID uuid = player.getUniqueId();

        Profile profile = cache.get(uuid);

        if (profile == null) {
            return;
        }

        updatePlaytime(uuid);

        repository.save(profile).thenRun(() -> {
            if (module.configBoolean("config.yml", "profile.cache.remove-on-quit", true)) {
                cache.remove(uuid);
            }

            module.logger().info(module, "Perfil descargado: " + player.getName());
        });
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            saveProfile(uuid);
        }
    }

    public void saveAllNow() {
        for (UUID uuid : cache.keySet()) {
            updatePlaytime(uuid);
        }

        for (Profile profile : cache.values()) {
            repository.save(profile).join();
        }
    }

    public void clear() {
        cache.clear();
        sessionStart.clear();
    }

    @Override
    public Optional<ProfileView> profile(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public Optional<Profile> fullProfile(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public Optional<Profile> fullProfile(Player player) {
        return fullProfile(player.getUniqueId());
    }

    @Override
    public boolean loaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public void updatePlaytime(UUID uuid) {
        Profile profile = cache.get(uuid);

        if (profile == null) {
            return;
        }

        Instant startedAt = sessionStart.get(uuid);

        if (startedAt == null) {
            sessionStart.put(uuid, Instant.now());
            return;
        }

        long seconds = Instant.now().getEpochSecond() - startedAt.getEpochSecond();

        if (seconds <= 0) {
            return;
        }

        profile.addPlaytimeSeconds(seconds);
        sessionStart.put(uuid, Instant.now());
    }
}