package net.ariatus.project.message;

import net.ariatus.project.AriatusCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessagesManager {

    private final AriatusCore core;
    private File file;
    private YamlConfiguration config;

    public MessagesManager(AriatusCore core) {
        this.core = core;
    }

    public void load() {
        file = new File(core.getDataFolder(), "messages.yml");

        if (!file.exists()) {
            core.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        load();
    }

    public String get(String path) {
        return config.getString(path, path);
    }

    public String prefixed(String path) {
        return get("prefix") + get(path);
    }

    public String replace(String message, String key, String value) {
        return message.replace("%" + key + "%", value);
    }
}