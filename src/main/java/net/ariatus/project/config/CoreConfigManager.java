package net.ariatus.project.config;

import net.ariatus.project.AriatusCore;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Objects;

public final class CoreConfigManager {

    private final AriatusCore core;

    public CoreConfigManager(AriatusCore core) {
        this.core = Objects.requireNonNull(core, "core");
    }

    public void load() {
        core.saveDefaultConfig();
        core.reloadConfig();
    }

    public void reload() {
        core.reloadConfig();
    }

    public FileConfiguration config() {
        return core.getConfig();
    }

    public String getString(String path, String def) {
        return config().getString(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return config().getBoolean(path, def);
    }

    public int getInt(String path, int def) {
        return config().getInt(path, def);
    }

    public long getLong(String path, long def) {
        return config().getLong(path, def);
    }

    public double getDouble(String path, double def) {
        return config().getDouble(path, def);
    }

    public List<String> getStringList(String path) {
        return List.copyOf(config().getStringList(path));
    }

    public boolean contains(String path) {
        return config().contains(path);
    }
}