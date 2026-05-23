package net.ariatus.project.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProfileMetadata {

    private final Map<String, String> values = new HashMap<>();

    public ProfileMetadata(Map<String, String> values) {
        if (values != null) {
            this.values.putAll(values);
        }
    }

    public static ProfileMetadata empty() {
        return new ProfileMetadata(Map.of());
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key.toLowerCase()));
    }

    public String getString(String key, String def) {
        return get(key).orElse(def);
    }

    public int getInt(String key, int def) {
        try {
            return Integer.parseInt(getString(key, String.valueOf(def)));
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(getString(key, String.valueOf(def)));
    }

    public void set(String key, String value) {
        values.put(key.toLowerCase(), value);
    }

    public void remove(String key) {
        values.remove(key.toLowerCase());
    }

    public boolean contains(String key) {
        return values.containsKey(key.toLowerCase());
    }

    public Map<String, String> values() {
        return Map.copyOf(values);
    }
}