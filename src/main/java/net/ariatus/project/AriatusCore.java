package net.ariatus.project;

import net.ariatus.project.command.AriatusCommand;
import net.ariatus.project.command.AriatusCommandManager;
import net.ariatus.project.config.AriatusConfigManager;
import net.ariatus.project.database.DatabaseService;
import net.ariatus.project.database.migration.MigrationManager;
import net.ariatus.project.database.migration.core.CreateCoreTablesMigration;
import net.ariatus.project.event.InternalEventBus;
import net.ariatus.project.event.ModuleDisabledEvent;
import net.ariatus.project.event.ModuleEnabledEvent;
import net.ariatus.project.event.ModuleReloadedEvent;
import net.ariatus.project.listener.AriatusListenerManager;
import net.ariatus.project.logger.LoggerService;
import net.ariatus.project.module.ModuleManager;
import net.ariatus.project.module.internal.TestModule;
import net.ariatus.project.module.loader.AriatusModuleLoader;
import net.ariatus.project.profiler.ModuleProfiler;
import net.ariatus.project.service.ServiceRegistry;
import net.ariatus.project.task.AriatusTaskManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AriatusCore extends JavaPlugin {

    private LoggerService loggerService;
    private InternalEventBus eventBus;
    private DatabaseService databaseService;
    private MigrationManager migrationManager;

    private ServiceRegistry serviceRegistry;

    private ModuleManager moduleManager;
    private AriatusModuleLoader moduleLoader;
    private AriatusTaskManager taskManager;
    private AriatusListenerManager listenerManager;
    private AriatusCommandManager commandManager;

    private AriatusConfigManager configManager;
    private ModuleProfiler profiler;

    @Override
    public void onEnable() {
        getLogger().info("Iniciando AriatusCore...");

        this.configManager = new AriatusConfigManager(this);
        this.configManager.load();

        this.loggerService = new LoggerService(this);
        this.eventBus = new InternalEventBus(this);

        this.databaseService = new DatabaseService(this);
        this.databaseService.connect();

        this.migrationManager = new MigrationManager(this);
        registerMigrations();
        migrationManager.runMigrations();

        this.profiler = new ModuleProfiler();

        this.serviceRegistry = new ServiceRegistry();

        this.moduleManager = new ModuleManager(this);
        this.moduleLoader = new AriatusModuleLoader(this);
        this.taskManager = new AriatusTaskManager(this);
        this.listenerManager = new AriatusListenerManager(this);
        this.commandManager = new AriatusCommandManager(this);

        registerServices();
        registerInternalEventListeners();
        registerModules();
        moduleLoader.discoverModules();
        moduleLoader.loadModules();
        registerCommands();
        enableConfiguredModules();

        loggerService().info("AriatusCore activado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Apagando AriatusCore...");

        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (taskManager != null) {
            taskManager.cancelAll();
        }
        if (listenerManager != null) {
            listenerManager.unregisterAll();
        }
        if (commandManager != null) {
            commandManager.unregisterAll();
        }
        if (serviceRegistry != null) {
            serviceRegistry.clear();
        }
        if (eventBus != null) {
            eventBus.clear();
        }
        if (databaseService != null) {
            databaseService.shutdown();
        }

        getLogger().info("AriatusCore apagado correctamente.");
    }

    private void registerServices() {
        serviceRegistry.register(AriatusConfigManager.class, configManager);
        serviceRegistry.register(LoggerService.class, loggerService);
        serviceRegistry.register(InternalEventBus.class, eventBus);
        serviceRegistry.register(DatabaseService.class, databaseService);
        serviceRegistry.register(MigrationManager.class, migrationManager);
        serviceRegistry.register(ModuleProfiler.class, profiler);
        serviceRegistry.register(AriatusTaskManager.class, taskManager);
        serviceRegistry.register(AriatusListenerManager.class, listenerManager);
        serviceRegistry.register(AriatusCommandManager.class, commandManager);
        serviceRegistry.register(ModuleManager.class, moduleManager);
        serviceRegistry.register(AriatusModuleLoader.class, moduleLoader);
    }

    private void registerMigrations() {
        migrationManager.register(new CreateCoreTablesMigration());
    }

    private void registerModules() {
        moduleManager.register(new TestModule(this));
    }

    private void registerCommands() {
        getCommand("ariatus").setExecutor(new AriatusCommand(moduleManager));
    }

    private void enableConfiguredModules() {
        for (String moduleId : getConfig().getStringList("modules.auto-enable")) {
            moduleManager.enable(moduleId);
        }
    }

    public ServiceRegistry services() {
        return serviceRegistry;
    }

    public ModuleManager moduleManager() {
        return moduleManager;
    }

    public AriatusTaskManager taskManager() {
        return taskManager;
    }

    public AriatusListenerManager listenerManager() {
        return listenerManager;
    }

    public AriatusCommandManager commandManager() {
        return commandManager;
    }

    public AriatusConfigManager configManager() {
        return configManager;
    }

    public ModuleProfiler profiler() {
        return profiler;
    }

    public LoggerService loggerService() {
        return loggerService;
    }

    public InternalEventBus eventBus() {
        return eventBus;
    }

    public DatabaseService databaseService() {
        return databaseService;
    }

    public MigrationManager migrationManager() {
        return migrationManager;
    }

    public AriatusModuleLoader moduleLoader() {
        return moduleLoader;
    }

    //
    //
    //

    private void registerInternalEventListeners() {
        eventBus.subscribe(ModuleEnabledEvent.class, event ->
                loggerService.info("Evento interno: módulo activado -> " + event.module().id())
        );

        eventBus.subscribe(ModuleDisabledEvent.class, event ->
                loggerService.info("Evento interno: módulo desactivado -> " + event.module().id())
        );

        eventBus.subscribe(ModuleReloadedEvent.class, event ->
                loggerService.info("Evento interno: módulo recargado -> " + event.module().id())
        );
    }

}