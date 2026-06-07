package net.ariatus.project;

import net.ariatus.project.api.storage.StorageService;
import net.ariatus.project.commands.StorageCommand;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.storage.MariaDBStorageRepository;
import net.ariatus.project.storage.StorageConfig;
import net.ariatus.project.storage.StorageListener;
import net.ariatus.project.storage.StorageManager;
import net.ariatus.project.storage.StorageRepository;

public class AriatusStorage extends ExternalAriatusModule {

    private ModuleStatus status = ModuleStatus.DISABLED;

    private StorageConfig storageConfig;
    private StorageRepository repository;
    private StorageManager storageManager;

    @Override
    public String id() {
        return "storage";
    }

    @Override
    public String name() {
        return "AriatusStorage";
    }

    @Override
    public void enable() {
        status = ModuleStatus.ENABLING;

        loadConfig("config.yml");

        this.storageConfig = new StorageConfig(this);
        this.repository = new MariaDBStorageRepository(this);
        this.storageManager = new StorageManager(this, repository, storageConfig);

        services().register(StorageService.class, storageManager);

        listeners().register(this, new StorageListener(this, storageManager));
        commands().register(this, new StorageCommand(storageManager));

        status = ModuleStatus.ENABLED;
        logger().info(this, "AriatusStorage activado correctamente.");
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLING;

        status = ModuleStatus.DISABLED;
        logger().info(this, "AriatusStorage desactivado correctamente.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }
}