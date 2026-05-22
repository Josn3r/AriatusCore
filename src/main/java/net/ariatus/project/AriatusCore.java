package net.ariatus.project;

import net.ariatus.project.command.AriatusCommand;
import net.ariatus.project.command.AriatusCommandManager;
import net.ariatus.project.command.AriatusTabCompleter;
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
import net.ariatus.project.message.MessagesManager;
import net.ariatus.project.module.ModuleManager;
import net.ariatus.project.module.config.ModuleConfigManager;
import net.ariatus.project.module.data.ModuleDataManager;
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
    private ModuleDataManager moduleDataManager;
    private ModuleConfigManager moduleConfigManager;

    private AriatusConfigManager configManager;
    private MessagesManager messagesManager;
    private ModuleProfiler profiler;

    @Override
    public void onEnable() {
        getLogger().info("Iniciando AriatusCore...");

        this.configManager = new AriatusConfigManager(this);
        this.configManager.load();

        this.messagesManager = new MessagesManager(this);
        this.messagesManager.load();

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
        this.moduleDataManager = new ModuleDataManager(this);
        this.moduleDataManager.load();
        this.moduleConfigManager = new ModuleConfigManager(this);
        this.moduleLoader = new AriatusModuleLoader(this);
        this.taskManager = new AriatusTaskManager(this);
        this.listenerManager = new AriatusListenerManager(this);
        this.commandManager = new AriatusCommandManager(this);


        registerServices();
        registerInternalEventListeners();
        if (configManager.getBoolean("modules.external.auto-load", true)) {
            moduleLoader.discoverModules();
            moduleLoader.loadModules();
        }
        registerCommands();
        enableConfiguredModules();

        loggerService().info("AriatusCore activado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Apagando AriatusCore...");

        if (moduleLoader != null) {
            moduleLoader.unloadAll();
        }

        if (moduleManager != null) {
            moduleManager.disableAll();
        }

        if (commandManager != null) {
            commandManager.unregisterAll();
        }

        if (listenerManager != null) {
            listenerManager.unregisterAll();
        }

        if (taskManager != null) {
            taskManager.cancelAll();
        }

        if (moduleConfigManager != null) {
            moduleConfigManager.unloadAll();
        }

        if (databaseService != null) {
            databaseService.shutdown();
        }

        if (eventBus != null) {
            eventBus.clear();
        }

        if (serviceRegistry != null) {
            serviceRegistry.clear();
        }

        getLogger().info("AriatusCore apagado correctamente.");
    }

    private void registerServices() {
        serviceRegistry.register(AriatusConfigManager.class, configManager);
        serviceRegistry.register(MessagesManager.class, messagesManager);
        serviceRegistry.register(LoggerService.class, loggerService);
        serviceRegistry.register(InternalEventBus.class, eventBus);
        serviceRegistry.register(DatabaseService.class, databaseService);
        serviceRegistry.register(MigrationManager.class, migrationManager);
        serviceRegistry.register(ModuleProfiler.class, profiler);
        serviceRegistry.register(AriatusTaskManager.class, taskManager);
        serviceRegistry.register(AriatusListenerManager.class, listenerManager);
        serviceRegistry.register(AriatusCommandManager.class, commandManager);
        serviceRegistry.register(ModuleManager.class, moduleManager);
        serviceRegistry.register(ModuleDataManager.class, moduleDataManager);
        serviceRegistry.register(ModuleConfigManager.class, moduleConfigManager);
        serviceRegistry.register(AriatusModuleLoader.class, moduleLoader);
    }

    private void registerMigrations() {
        migrationManager.register(new CreateCoreTablesMigration());
    }

    private void registerCommands() {
        var command = getCommand("ariatus");

        if (command == null) {
            loggerService.error("No se pudo registrar el comando /ariatus.");
            return;
        }

        command.setExecutor(new AriatusCommand(moduleManager));
        command.setTabCompleter(new AriatusTabCompleter(moduleManager));
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

    public ModuleDataManager moduleDataManager() {
        return moduleDataManager;
    }

    public ModuleConfigManager moduleConfigManager() {
        return moduleConfigManager;
    }

    public MessagesManager messages() {
        return messagesManager;
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