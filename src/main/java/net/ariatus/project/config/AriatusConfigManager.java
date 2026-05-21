package net.ariatus.project.config;

import net.ariatus.project.AriatusCore;
import org.bukkit.configuration.file.FileConfiguration;

public class AriatusConfigManager {

    private final AriatusCore core;

    public AriatusConfigManager(AriatusCore core) {
        this.core = core;
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
}