package net.ariatus.project.module.runtime;

import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.module.config.ModuleConfig;
import net.ariatus.project.module.config.ModuleConfigManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Objects;

public final class ModuleConfigs {

    private final AriatusModule module;
    private final ModuleConfigManager manager;

    public ModuleConfigs(AriatusModule module, ModuleConfigManager manager) {
        this.module = Objects.requireNonNull(module, "module");
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public FileConfiguration main() {
        return file("config.yml");
    }

    public FileConfiguration file(String fileName) {
        ModuleConfig config = manager.get(module, fileName);

        if (config == null) {
            config = manager.load(module, fileName);
        }

        return config.configuration();
    }

    public FileConfiguration reload() {
        return reload("config.yml");
    }

    public FileConfiguration reload(String fileName) {
        return manager.reload(module, fileName).configuration();
    }

    public boolean save() {
        return manager.save(module);
    }

    public boolean save(String fileName) {
        return manager.save(module, fileName);
    }

    public int saveAll() {
        return manager.saveAll(module);
    }

    public void saveDefault(String fileName) {
        manager.saveDefaultResource(module, fileName);
    }

    public boolean exists(String fileName) {
        return manager.exists(module, fileName);
    }

    public File path(String fileName) {
        return manager.file(module, fileName);
    }

    public int loaded() {
        return manager.loaded(module);
    }
}