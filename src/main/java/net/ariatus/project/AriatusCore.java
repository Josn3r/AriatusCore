package net.ariatus.project;

import net.ariatus.project.command.AriatusCommandManager;
import net.ariatus.project.config.CoreConfigManager;
import net.ariatus.project.database.DatabaseService;
import net.ariatus.project.database.migration.MigrationManager;
import net.ariatus.project.event.InternalEventBus;
import net.ariatus.project.listener.AriatusListenerManager;
import net.ariatus.project.logger.LoggerService;
import net.ariatus.project.message.MessagesManager;
import net.ariatus.project.module.ModuleManager;
import net.ariatus.project.module.config.ModuleConfigManager;
import net.ariatus.project.module.data.ModuleDataManager;
import net.ariatus.project.module.loader.AriatusModuleLoader;
import net.ariatus.project.profiler.ModuleProfiler;
import net.ariatus.project.runtime.AriatusRuntime;
import net.ariatus.project.runtime.AriatusRuntimeState;
import net.ariatus.project.service.ServiceRegistry;
import net.ariatus.project.task.AriatusTaskManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AriatusCore extends JavaPlugin {

    private AriatusRuntime runtime;

    @Override
    public void onEnable() {
        runtime =
                new AriatusRuntime(
                        this
                );

        try {
            runtime.start();

        } catch (Throwable throwable) {
            getLogger().severe(
                    "AriatusCore no pudo iniciar correctamente."
            );

            getServer()
                    .getPluginManager()
                    .disablePlugin(
                            this
                    );
        }
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.stop();
        }
    }

    public AriatusRuntime runtime() {
        AriatusRuntime current =
                runtime;

        if (current == null) {
            throw new IllegalStateException(
                    "AriatusRuntime todavía no fue creado."
            );
        }

        return current;
    }

    public AriatusRuntimeState runtimeState() {
        AriatusRuntime current =
                runtime;

        return current == null
                ? AriatusRuntimeState.STOPPED
                : current.state();
    }

    public CoreConfigManager configManager() {
        return runtime().configManager();
    }

    public MessagesManager messages() {
        return runtime().messages();
    }

    public LoggerService loggerService() {
        return runtime().loggerService();
    }

    public InternalEventBus eventBus() {
        return runtime().eventBus();
    }

    public DatabaseService databaseService() {
        return runtime().databaseService();
    }

    public MigrationManager migrationManager() {
        return runtime().migrationManager();
    }

    public ModuleProfiler profiler() {
        return runtime().profiler();
    }

    public ServiceRegistry services() {
        return runtime().services();
    }

    public ModuleManager moduleManager() {
        return runtime().moduleManager();
    }

    public ModuleDataManager moduleDataManager() {
        return runtime().moduleDataManager();
    }

    public ModuleConfigManager moduleConfigManager() {
        return runtime().moduleConfigManager();
    }

    public AriatusTaskManager taskManager() {
        return runtime().taskManager();
    }

    public AriatusListenerManager listenerManager() {
        return runtime().listenerManager();
    }

    public AriatusCommandManager commandManager() {
        return runtime().commandManager();
    }

    public AriatusModuleLoader moduleLoader() {
        return runtime().moduleLoader();
    }
}