package net.ariatus.project.runtime;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.command.AriatusCommand;
import net.ariatus.project.command.AriatusCommandManager;
import net.ariatus.project.command.AriatusTabCompleter;
import net.ariatus.project.config.CoreConfigManager;
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
import org.bukkit.command.PluginCommand;

import java.util.Objects;

public final class AriatusRuntime {

    private final AriatusCore core;

    private volatile AriatusRuntimeState state = AriatusRuntimeState.STOPPED;

    private CoreConfigManager configManager;
    private MessagesManager messagesManager;

    private LoggerService loggerService;
    private InternalEventBus eventBus;

    private DatabaseService databaseService;
    private MigrationManager migrationManager;

    private ModuleProfiler profiler;
    private ServiceRegistry serviceRegistry;

    private ModuleManager moduleManager;
    private ModuleDataManager moduleDataManager;
    private ModuleConfigManager moduleConfigManager;

    private AriatusTaskManager taskManager;
    private AriatusListenerManager listenerManager;
    private AriatusCommandManager commandManager;

    private AriatusModuleLoader moduleLoader;

    public AriatusRuntime(AriatusCore core) {
        this.core = Objects.requireNonNull(
                core,
                "core"
        );
    }

    public synchronized void start() {
        if (
                state == AriatusRuntimeState.STARTING
                        || state == AriatusRuntimeState.RUNNING
        ) {
            throw new IllegalStateException(
                    "AriatusRuntime ya está iniciado o iniciándose."
            );
        }

        if (state == AriatusRuntimeState.STOPPING) {
            throw new IllegalStateException(
                    "AriatusRuntime se está apagando."
            );
        }

        state = AriatusRuntimeState.STARTING;

        core.getLogger().info(
                "Iniciando AriatusRuntime..."
        );

        try {
            initializeConfiguration();
            initializeLogging();
            initializeDatabase();
            initializeModuleRuntime();

            registerCoreServices();
            registerMigrations();
            registerInternalEvents();
            registerCoreCommands();

            runMigrations();
            loadModules();

            state = AriatusRuntimeState.RUNNING;

            loggerService.info(
                    "AriatusRuntime iniciado correctamente."
            );

        } catch (Throwable throwable) {
            state = AriatusRuntimeState.FAILED;

            logStartupFailure(
                    throwable
            );

            shutdownComponents();

            state = AriatusRuntimeState.FAILED;

            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new IllegalStateException(
                    "No se pudo iniciar AriatusRuntime.",
                    throwable
            );
        }
    }

    public synchronized void stop() {
        if (
                state == AriatusRuntimeState.STOPPED
                        || state == AriatusRuntimeState.STOPPING
        ) {
            return;
        }

        state = AriatusRuntimeState.STOPPING;

        logInfo(
                "Apagando AriatusRuntime..."
        );

        shutdownComponents();

        state = AriatusRuntimeState.STOPPED;

        core.getLogger().info(
                "AriatusRuntime apagado correctamente."
        );
    }

    public AriatusRuntimeState state() {
        return state;
    }

    public boolean isRunning() {
        return state == AriatusRuntimeState.RUNNING;
    }

    public CoreConfigManager configManager() {
        return requireAvailable(
                configManager,
                "CoreConfigManager"
        );
    }

    public MessagesManager messages() {
        return requireAvailable(
                messagesManager,
                "MessagesManager"
        );
    }

    public LoggerService loggerService() {
        return requireAvailable(
                loggerService,
                "LoggerService"
        );
    }

    public InternalEventBus eventBus() {
        return requireAvailable(
                eventBus,
                "InternalEventBus"
        );
    }

    public DatabaseService databaseService() {
        return requireAvailable(
                databaseService,
                "DatabaseService"
        );
    }

    public MigrationManager migrationManager() {
        return requireAvailable(
                migrationManager,
                "MigrationManager"
        );
    }

    public ModuleProfiler profiler() {
        return requireAvailable(
                profiler,
                "ModuleProfiler"
        );
    }

    public ServiceRegistry services() {
        return requireAvailable(
                serviceRegistry,
                "ServiceRegistry"
        );
    }

    public ModuleManager moduleManager() {
        return requireAvailable(
                moduleManager,
                "ModuleManager"
        );
    }

    public ModuleDataManager moduleDataManager() {
        return requireAvailable(
                moduleDataManager,
                "ModuleDataManager"
        );
    }

    public ModuleConfigManager moduleConfigManager() {
        return requireAvailable(
                moduleConfigManager,
                "ModuleConfigManager"
        );
    }

    public AriatusTaskManager taskManager() {
        return requireAvailable(
                taskManager,
                "AriatusTaskManager"
        );
    }

    public AriatusListenerManager listenerManager() {
        return requireAvailable(
                listenerManager,
                "AriatusListenerManager"
        );
    }

    public AriatusCommandManager commandManager() {
        return requireAvailable(
                commandManager,
                "AriatusCommandManager"
        );
    }

    public AriatusModuleLoader moduleLoader() {
        return requireAvailable(
                moduleLoader,
                "AriatusModuleLoader"
        );
    }

    private void initializeConfiguration() {
        configManager =
                new CoreConfigManager(
                        core
                );

        configManager.load();

        messagesManager =
                new MessagesManager(
                        core
                );

        messagesManager.load();
    }

    private void initializeLogging() {
        loggerService =
                new LoggerService(
                        core
                );

        eventBus =
                new InternalEventBus(
                        core
                );

        loggerService.debug(
                "Logging e InternalEventBus inicializados."
        );
    }

    private void initializeDatabase() {
        databaseService =
                new DatabaseService(
                        core
                );

        databaseService.connect();

        migrationManager =
                new MigrationManager(
                        core
                );
    }

    private void initializeModuleRuntime() {
        profiler =
                new ModuleProfiler();

        serviceRegistry =
                new ServiceRegistry();

        moduleManager =
                new ModuleManager(
                        core
                );

        moduleDataManager =
                new ModuleDataManager(
                        core
                );

        moduleDataManager.load();

        moduleConfigManager =
                new ModuleConfigManager(
                        core
                );

        taskManager =
                new AriatusTaskManager(
                        core
                );

        listenerManager =
                new AriatusListenerManager(
                        core
                );

        commandManager =
                new AriatusCommandManager(
                        core
                );

        moduleLoader =
                new AriatusModuleLoader(
                        core
                );
    }

    private void registerCoreServices() {
        serviceRegistry.registerCore(
                AriatusRuntime.class,
                this
        );

        serviceRegistry.registerCore(
                CoreConfigManager.class,
                configManager
        );

        serviceRegistry.registerCore(
                MessagesManager.class,
                messagesManager
        );

        serviceRegistry.registerCore(
                LoggerService.class,
                loggerService
        );

        serviceRegistry.registerCore(
                InternalEventBus.class,
                eventBus
        );

        serviceRegistry.registerCore(
                DatabaseService.class,
                databaseService
        );

        serviceRegistry.registerCore(
                MigrationManager.class,
                migrationManager
        );

        serviceRegistry.registerCore(
                ModuleProfiler.class,
                profiler
        );

        serviceRegistry.registerCore(
                ModuleManager.class,
                moduleManager
        );

        serviceRegistry.registerCore(
                ModuleDataManager.class,
                moduleDataManager
        );

        serviceRegistry.registerCore(
                ModuleConfigManager.class,
                moduleConfigManager
        );

        serviceRegistry.registerCore(
                AriatusTaskManager.class,
                taskManager
        );

        serviceRegistry.registerCore(
                AriatusListenerManager.class,
                listenerManager
        );

        serviceRegistry.registerCore(
                AriatusCommandManager.class,
                commandManager
        );

        serviceRegistry.registerCore(
                AriatusModuleLoader.class,
                moduleLoader
        );
    }

    private void registerMigrations() {
        migrationManager.register(
                new CreateCoreTablesMigration()
        );
    }

    private void registerInternalEvents() {
        eventBus.subscribe(
                ModuleEnabledEvent.class,
                event -> loggerService.debug(
                        "Módulo activado -> "
                                + event.module().id()
                )
        );

        eventBus.subscribe(
                ModuleDisabledEvent.class,
                event -> loggerService.debug(
                        "Módulo desactivado -> "
                                + event.module().id()
                )
        );

        eventBus.subscribe(
                ModuleReloadedEvent.class,
                event -> loggerService.debug(
                        "Módulo recargado -> "
                                + event.module().id()
                )
        );
    }

    private void registerCoreCommands() {
        PluginCommand command =
                core.getCommand(
                        "ariatus"
                );

        if (command == null) {
            throw new IllegalStateException(
                    "El comando /ariatus no está definido en plugin.yml."
            );
        }

        command.setExecutor(
                new AriatusCommand(
                        moduleManager
                )
        );

        command.setTabCompleter(
                new AriatusTabCompleter(
                        moduleManager
                )
        );
    }

    private void runMigrations() {
        migrationManager.runMigrations();
    }

    private void loadModules() {
        if (
                !configManager.getBoolean(
                        "modules.external.auto-load",
                        true
                )
        ) {
            loggerService.info(
                    "Carga automática de módulos desactivada."
            );

            return;
        }

        moduleLoader.discoverModules();
        moduleLoader.loadModules();

        enableConfiguredModules();
    }

    private void enableConfiguredModules() {
        for (
                String moduleId :
                configManager.getStringList(
                        "modules.auto-enable"
                )
        ) {
            if (
                    !moduleManager.enable(
                            moduleId
                    )
            ) {
                loggerService.warn(
                        "No se pudo activar automáticamente el módulo "
                                + moduleId
                                + "."
                );
            }
        }
    }

    private void shutdownComponents() {
        shutdownStep(
                "módulos externos",
                () -> {
                    if (moduleLoader != null) {
                        moduleLoader.unloadAll();
                    }
                }
        );

        shutdownStep(
                "ModuleManager",
                () -> {
                    if (moduleManager != null) {
                        moduleManager.disableAll();
                    }
                }
        );

        shutdownStep(
                "comandos dinámicos",
                () -> {
                    if (commandManager != null) {
                        commandManager.unregisterAll();
                    }
                }
        );

        shutdownStep(
                "listeners",
                () -> {
                    if (listenerManager != null) {
                        listenerManager.unregisterAll();
                    }
                }
        );

        shutdownStep(
                "tasks",
                () -> {
                    if (taskManager != null) {
                        taskManager.cancelAll();
                    }
                }
        );

        shutdownStep(
                "configs de módulos",
                () -> {
                    if (moduleConfigManager != null) {
                        moduleConfigManager.unloadAll();
                    }
                }
        );

        shutdownStep(
                "event bus",
                () -> {
                    if (eventBus != null) {
                        eventBus.clear();
                    }
                }
        );

        shutdownStep(
                "service registry",
                () -> {
                    if (serviceRegistry != null) {
                        serviceRegistry.clear();
                    }
                }
        );

        shutdownStep(
                "database",
                () -> {
                    if (databaseService != null) {
                        databaseService.shutdown();
                    }
                }
        );

        clearReferences();
    }

    private void clearReferences() {
        moduleLoader = null;

        commandManager = null;
        listenerManager = null;
        taskManager = null;

        moduleConfigManager = null;
        moduleDataManager = null;
        moduleManager = null;

        serviceRegistry = null;
        profiler = null;

        migrationManager = null;
        databaseService = null;

        eventBus = null;
        loggerService = null;

        messagesManager = null;
        configManager = null;
    }

    private void shutdownStep(
            String component,
            Runnable action
    ) {
        try {
            action.run();

        } catch (Throwable throwable) {
            if (loggerService != null) {
                loggerService.error(
                        "Error apagando "
                                + component
                                + ".",
                        throwable
                );

                return;
            }

            core.getLogger().severe(
                    "Error apagando "
                            + component
                            + ": "
                            + throwable.getMessage()
            );
        }
    }

    private void logStartupFailure(
            Throwable throwable
    ) {
        if (loggerService != null) {
            loggerService.error(
                    "AriatusRuntime no pudo iniciarse.",
                    throwable
            );

            return;
        }

        core.getLogger().severe(
                "AriatusRuntime no pudo iniciarse: "
                        + throwable.getMessage()
        );
    }

    private void logInfo(
            String message
    ) {
        if (loggerService != null) {
            loggerService.info(
                    message
            );

            return;
        }

        core.getLogger().info(
                message
        );
    }

    private <T> T requireAvailable(
            T component,
            String componentName
    ) {
        if (component == null) {
            throw new IllegalStateException(
                    componentName
                            + " todavía no está disponible en AriatusRuntime. Estado actual: "
                            + state
            );
        }

        return component;
    }
}