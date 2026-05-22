package net.ariatus.project.module;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.command.AriatusCommandManager;
import net.ariatus.project.database.DatabaseService;
import net.ariatus.project.listener.AriatusListenerManager;
import net.ariatus.project.logger.LoggerService;
import net.ariatus.project.module.config.ModuleConfig;
import net.ariatus.project.service.ServiceRegistry;
import net.ariatus.project.task.AriatusTaskManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public abstract class ExternalAriatusModule implements AriatusModule {

    private AriatusCore core;

    public void initialize(AriatusCore core) {
        this.core = core;
    }

    public AriatusCore core() {
        return core;
    }

    public File dataFolder() {
        return core.moduleDataManager().folder(this);
    }

    public ModuleConfig loadConfig() {
        return core.moduleConfigManager().load(this);
    }

    public ModuleConfig loadConfig(String fileName) {
        return core.moduleConfigManager().load(this, fileName);
    }

    public ModuleConfig reloadConfig() {
        return core.moduleConfigManager().reload(this);
    }

    public ModuleConfig reloadConfig(String fileName) {
        return core.moduleConfigManager().reload(this, fileName);
    }

    public FileConfiguration config() {
        ModuleConfig moduleConfig = core.moduleConfigManager().get(this);

        if (moduleConfig == null) {
            moduleConfig = loadConfig();
        }

        return moduleConfig.configuration();
    }

    public FileConfiguration config(String fileName) {
        ModuleConfig moduleConfig = core.moduleConfigManager().get(this, fileName);

        if (moduleConfig == null) {
            moduleConfig = loadConfig(fileName);
        }

        return moduleConfig.configuration();
    }

    public String configString(String path, String def) {
        return config().getString(path, def);
    }

    public String configString(String fileName, String path, String def) {
        return config(fileName).getString(path, def);
    }

    public boolean configBoolean(String path, boolean def) {
        return config().getBoolean(path, def);
    }

    public boolean configBoolean(String fileName, String path, boolean def) {
        return config(fileName).getBoolean(path, def);
    }

    public int configInt(String path, int def) {
        return config().getInt(path, def);
    }

    public int configInt(String fileName, String path, int def) {
        return config(fileName).getInt(path, def);
    }

    public double configDouble(String path, double def) {
        return config().getDouble(path, def);
    }

    public double configDouble(String fileName, String path, double def) {
        return config(fileName).getDouble(path, def);
    }

    public AriatusTaskManager tasks() {
        return core.taskManager();
    }

    public AriatusListenerManager listeners() {
        return core.listenerManager();
    }

    public AriatusCommandManager commands() {
        return core.commandManager();
    }

    public DatabaseService database() {
        return core.databaseService();
    }

    public LoggerService logger() {
        return core.loggerService();
    }

    public ServiceRegistry services() {
        return core.services();
    }
}