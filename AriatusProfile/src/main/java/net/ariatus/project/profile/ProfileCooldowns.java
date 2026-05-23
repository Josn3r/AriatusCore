package net.ariatus.project.profile;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ProfileCooldowns {

    private final Map<String, Long> cooldowns = new HashMap<>();

    public ProfileCooldowns(Map<String, Long> cooldowns) {
        if (cooldowns != null) {
            this.cooldowns.putAll(cooldowns);
        }
    }

    public static ProfileCooldowns empty() {
        return new ProfileCooldowns(Map.of());
    }

    public boolean active(String key) {
        return expiresAt(key) > Instant.now().getEpochSecond();
    }

    public long expiresAt(String key) {
        return cooldowns.getOrDefault(key.toLowerCase(), 0L);
    }

    public long remainingSeconds(String key) {
        long remaining = expiresAt(key) - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    public void set(String key, long secondsFromNow) {
        long expiresAt = Instant.now().getEpochSecond() + Math.max(0, secondsFromNow);
        cooldowns.put(key.toLowerCase(), expiresAt);
    }

    public void remove(String key) {
        cooldowns.remove(key.toLowerCase());
    }

    public Map<String, Long> values() {
        return Map.copyOf(cooldowns);
    }
}