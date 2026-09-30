package net.ariatus.project.module;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.resource.ManagedResourceRegistry;
import net.ariatus.project.module.runtime.ModuleCommands;
import net.ariatus.project.module.runtime.ModuleConfigs;
import net.ariatus.project.module.runtime.ModuleDatabase;
import net.ariatus.project.module.runtime.ModuleEvents;
import net.ariatus.project.module.runtime.ModuleListeners;
import net.ariatus.project.module.runtime.ModuleLogger;
import net.ariatus.project.module.runtime.ModuleResources;
import net.ariatus.project.module.runtime.ModuleServices;
import net.ariatus.project.module.runtime.ModuleTasks;

import java.io.File;
import java.util.Objects;

public final class ModuleContext {

    private final AriatusCore core;
    private final ModuleContainer container;

    private final ModuleLogger logger;

    private final ManagedResourceRegistry resourceRegistry;
    private final ModuleResources resources;

    private final ModuleServices services;
    private final ModuleTasks tasks;
    private final ModuleListeners listeners;
    private final ModuleCommands commands;
    private final ModuleEvents events;
    private final ModuleConfigs configs;
    private final ModuleDatabase database;

    ModuleContext(
            AriatusCore core,
            ModuleContainer container
    ) {
        this.core =
                Objects.requireNonNull(
                        core,
                        "core"
                );

        this.container =
                Objects.requireNonNull(
                        container,
                        "container"
                );

        AriatusModule module =
                container.module();

        this.logger =
                new ModuleLogger(
                        module,
                        core.loggerService()
                );

        this.resourceRegistry =
                new ManagedResourceRegistry();

        this.resources =
                new ModuleResources(
                        module,
                        resourceRegistry
                );

        this.services =
                new ModuleServices(
                        module,
                        core.services()
                );

        this.tasks =
                new ModuleTasks(
                        module,
                        core.taskManager()
                );

        this.listeners =
                new ModuleListeners(
                        module,
                        core.listenerManager()
                );

        this.commands =
                new ModuleCommands(
                        module,
                        core.commandManager()
                );

        this.events =
                new ModuleEvents(
                        module,
                        core.eventBus()
                );

        this.configs =
                new ModuleConfigs(
                        module,
                        core.moduleConfigManager()
                );

        this.database =
                new ModuleDatabase(
                        module,
                        core.databaseService(),
                        core.migrationManager()
                );
    }

    public AriatusCore core() {
        return core;
    }

    public ModuleContainer container() {
        return container;
    }

    public ModuleDescriptor descriptor() {
        return container.descriptor();
    }

    public String id() {
        return descriptor().id();
    }

    public String name() {
        return descriptor().name();
    }

    public String version() {
        return descriptor().version();
    }

    public ModuleStatus status() {
        return container.status();
    }

    public File dataFolder() {
        return core.moduleDataManager()
                .folder(
                        container.module()
                );
    }

    public ModuleLogger logger() {
        return logger;
    }

    public ModuleResources resources() {
        return resources;
    }

    public ModuleServices services() {
        return services;
    }

    public ModuleTasks tasks() {
        return tasks;
    }

    public ModuleListeners listeners() {
        return listeners;
    }

    public ModuleCommands commands() {
        return commands;
    }

    public ModuleEvents events() {
        return events;
    }

    public ModuleConfigs configs() {
        return configs;
    }

    public ModuleDatabase database() {
        return database;
    }

    void closeManagedResources() {
        AriatusModule module =
                container.module();

        closeUiResource(
                "dialogs",
                () ->
                        core.dialogUtils()
                                .release(module)
        );

        closeUiResource(
                "menus",
                () ->
                        core.menuUtils()
                                .release(module)
        );

        resourceRegistry.closeAll(
                logger
        );
    }

    private void closeUiResource(
            String resource,
            Runnable action
    ) {
        try {
            action.run();

        } catch (Throwable throwable) {
            logger.error(
                    "Error liberando "
                            + resource
                            + " del módulo.",
                    throwable
            );
        }
    }
}