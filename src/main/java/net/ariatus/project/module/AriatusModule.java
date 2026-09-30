package net.ariatus.project.module;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.integration.PlaceholderService;
import net.ariatus.project.module.runtime.ModuleCommands;
import net.ariatus.project.module.runtime.ModuleConfigs;
import net.ariatus.project.module.runtime.ModuleDatabase;
import net.ariatus.project.module.runtime.ModuleEvents;
import net.ariatus.project.module.runtime.ModuleListeners;
import net.ariatus.project.module.runtime.ModuleLogger;
import net.ariatus.project.module.runtime.ModuleResources;
import net.ariatus.project.module.runtime.ModuleServices;
import net.ariatus.project.module.runtime.ModuleTasks;
import net.ariatus.project.ui.dialog.DialogUtils;
import net.ariatus.project.ui.item.ItemUtils;
import net.ariatus.project.ui.menu.MenuUtils;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.List;
import java.util.Objects;

public abstract class AriatusModule {

    private ModuleContext context;

    public final void initialize(
            ModuleContext context
    ) {
        if (this.context != null) {
            throw new IllegalStateException(
                    "El módulo "
                            + this.context.id()
                            + " ya fue inicializado."
            );
        }

        this.context =
                Objects.requireNonNull(
                        context,
                        "context"
                );
    }

    protected void onEnable() throws Exception {
    }

    protected void onDisable() throws Exception {
    }

    public final ModuleContext context() {
        if (context == null) {
            throw new IllegalStateException(
                    "El módulo todavía no fue inicializado por AriatusCore."
            );
        }

        return context;
    }

    public final AriatusCore core() {
        return context().core();
    }

    public final ModuleDescriptor descriptor() {
        return context().descriptor();
    }

    public final String id() {
        return context().id();
    }

    public final String name() {
        return context().name();
    }

    public final String version() {
        return context().version();
    }

    public final ModuleStatus status() {
        return context().status();
    }

    public final List<String> dependencies() {
        return descriptor().dependencies();
    }

    public final List<String> softDependencies() {
        return descriptor().softDependencies();
    }

    public final File dataFolder() {
        return context().dataFolder();
    }

    public final ModuleLogger logger() {
        return context().logger();
    }

    public final ModuleResources resources() {
        return context().resources();
    }

    public final ModuleServices services() {
        return context().services();
    }

    public final ModuleTasks tasks() {
        return context().tasks();
    }

    public final ModuleListeners listeners() {
        return context().listeners();
    }

    public final ModuleCommands commands() {
        return context().commands();
    }

    public final ModuleEvents events() {
        return context().events();
    }

    public final ModuleConfigs configs() {
        return context().configs();
    }

    public final ModuleDatabase database() {
        return context().database();
    }

    public final MenuUtils.Scope menus() {
        return core()
                .menuUtils()
                .scope(this);
    }

    public final DialogUtils.Scope dialogs() {
        return core()
                .dialogUtils()
                .scope(this);
    }

    public final ItemUtils items() {
        return core()
                .itemUtils();
    }

    public final PlaceholderService placeholders() {
        return core()
                .placeholderService();
    }

    public final FileConfiguration config() {
        return configs().main();
    }
}