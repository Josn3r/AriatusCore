package net.ariatus.project.module;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.command.AriatusCommandManager;
import net.ariatus.project.event.ModuleDisabledEvent;
import net.ariatus.project.event.ModuleEnabledEvent;
import net.ariatus.project.event.ModuleReloadedEvent;
import net.ariatus.project.listener.AriatusListenerManager;
import net.ariatus.project.task.AriatusTaskManager;
import org.bukkit.Bukkit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ModuleManager {

    private final AriatusCore core;
    private final Map<String, ModuleContainer> modules = new LinkedHashMap<>();

    public ModuleManager(AriatusCore core) {
        this.core = Objects.requireNonNull(
                core,
                "core"
        );
    }

    public void register(ModuleContainer container) {
        ensurePrimaryThread(
                "registrar módulos"
        );

        Objects.requireNonNull(
                container,
                "container"
        );

        String id =
                container.descriptor().id();

        if (
                modules.putIfAbsent(
                        id,
                        container
                ) != null
        ) {
            throw new IllegalStateException(
                    "Ya existe un módulo registrado con id: "
                            + id
            );
        }

        core.loggerService().info(
                "Módulo registrado: "
                        + container.descriptor().name()
                        + " ("
                        + id
                        + ")"
        );
    }

    public Optional<AriatusModule> getModule(
            String id
    ) {
        return getContainer(id)
                .map(
                        ModuleContainer::module
                );
    }

    public Optional<ModuleContainer> getContainer(
            String id
    ) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                modules.get(
                        normalizeId(id)
                )
        );
    }

    public Collection<AriatusModule> getModules() {
        return modules.values()
                .stream()
                .map(
                        ModuleContainer::module
                )
                .toList();
    }

    public Collection<ModuleContainer> getContainers() {
        return List.copyOf(
                modules.values()
        );
    }

    public boolean enable(String id) {
        ensurePrimaryThread(
                "activar módulos"
        );

        if (id == null) {
            return false;
        }

        return enable(
                normalizeId(id),
                new LinkedHashSet<>()
        );
    }

    public boolean disable(String id) {
        ensurePrimaryThread(
                "desactivar módulos"
        );

        if (id == null) {
            return false;
        }

        String moduleId =
                normalizeId(id);

        ModuleContainer container =
                modules.get(moduleId);

        if (container == null) {
            return false;
        }

        Optional<ModuleContainer> dependent =
                findEnabledRuntimeDependent(
                        moduleId
                );

        if (dependent.isPresent()) {
            core.loggerService().warn(
                    "No se puede desactivar "
                            + moduleId
                            + " porque el módulo activo "
                            + dependent.get()
                            .descriptor()
                            .id()
                            + " depende de él."
            );

            return false;
        }

        return disableContainer(
                container,
                true
        );
    }

    public boolean reload(String id) {
        ensurePrimaryThread(
                "recargar módulos"
        );

        Optional<ModuleContainer> optional =
                getContainer(id);

        if (optional.isEmpty()) {
            return false;
        }

        ModuleContainer container =
                optional.get();

        if (
                !disable(
                        container.descriptor().id()
                )
        ) {
            return false;
        }

        if (
                !enable(
                        container.descriptor().id()
                )
        ) {
            return false;
        }

        core.eventBus().publish(
                new ModuleReloadedEvent(
                        container.module()
                )
        );

        return true;
    }

    public boolean unregister(String id) {
        ensurePrimaryThread(
                "descargar módulos"
        );

        if (id == null) {
            return false;
        }

        String moduleId =
                normalizeId(id);

        ModuleContainer container =
                modules.get(moduleId);

        if (container == null) {
            return false;
        }

        Optional<ModuleContainer> dependent =
                findLoadedRuntimeDependent(
                        moduleId
                );

        if (dependent.isPresent()) {
            core.loggerService().warn(
                    "No se puede descargar "
                            + moduleId
                            + " porque el ClassLoader de "
                            + dependent.get()
                            .descriptor()
                            .id()
                            + " mantiene una dependencia hacia él."
            );

            return false;
        }

        if (
                container.status()
                        == ModuleStatus.ENABLED
        ) {
            if (
                    !disableContainer(
                            container,
                            true
                    )
            ) {
                core.loggerService().warn(
                        "El módulo "
                                + moduleId
                                + " produjo errores durante onDisable(), pero continuará su descarga."
                );
            }

        } else {
            cleanup(container);

            container.status(
                    ModuleStatus.DISABLED
            );
        }

        modules.remove(moduleId);

        core.loggerService().info(
                "Módulo eliminado del runtime: "
                        + moduleId
        );

        return true;
    }

    public void disableAll() {
        ensurePrimaryThread(
                "desactivar módulos"
        );

        for (
                String id :
                shutdownOrderIds()
        ) {
            ModuleContainer container =
                    modules.get(id);

            if (container == null) {
                continue;
            }

            if (
                    container.status()
                            == ModuleStatus.ENABLED
            ) {
                disableContainer(
                        container,
                        true
                );

            } else {
                cleanup(container);

                container.status(
                        ModuleStatus.DISABLED
                );
            }
        }
    }

    public List<String> startupOrderIds() {
        LinkedHashSet<String> visited =
                new LinkedHashSet<>();

        LinkedHashSet<String> visiting =
                new LinkedHashSet<>();

        List<String> order =
                new ArrayList<>();

        for (String id : modules.keySet()) {
            visitRuntimeDependencies(
                    id,
                    visiting,
                    visited,
                    order
            );
        }

        return List.copyOf(order);
    }

    public List<String> shutdownOrderIds() {
        List<String> order =
                new ArrayList<>(
                        startupOrderIds()
                );

        Collections.reverse(order);

        return List.copyOf(order);
    }

    public Set<String> runtimeDependentsClosure(
            String moduleId
    ) {
        String target =
                normalizeId(moduleId);

        if (!modules.containsKey(target)) {
            return Set.of();
        }

        LinkedHashSet<String> result =
                new LinkedHashSet<>();

        Deque<String> queue =
                new ArrayDeque<>();

        result.add(target);
        queue.add(target);

        while (!queue.isEmpty()) {
            String dependency =
                    queue.removeFirst();

            for (
                    ModuleContainer container :
                    modules.values()
            ) {
                String candidate =
                        container.descriptor().id();

                if (result.contains(candidate)) {
                    continue;
                }

                if (
                        container.classLoader()
                                .dependsOn(
                                        dependency
                                )
                ) {
                    result.add(candidate);
                    queue.addLast(candidate);
                }
            }
        }

        return Collections.unmodifiableSet(
                result
        );
    }

    public AriatusTaskManager taskManager() {
        return core.taskManager();
    }

    public AriatusListenerManager listenerManager() {
        return core.listenerManager();
    }

    public AriatusCommandManager commandManager() {
        return core.commandManager();
    }

    public AriatusCore core() {
        return core;
    }

    private boolean enable(
            String id,
            Set<String> chain
    ) {
        ModuleContainer container =
                modules.get(id);

        if (container == null) {
            return false;
        }

        if (
                container.status()
                        == ModuleStatus.ENABLED
        ) {
            return true;
        }

        if (
                container.status()
                        == ModuleStatus.ENABLING
        ) {
            core.loggerService().error(
                    "Dependencia circular durante enable: "
                            + String.join(
                            " -> ",
                            chain
                    )
                            + " -> "
                            + id
            );

            return false;
        }

        if (!chain.add(id)) {
            core.loggerService().error(
                    "Dependencia circular durante enable: "
                            + String.join(
                            " -> ",
                            chain
                    )
                            + " -> "
                            + id
            );

            return false;
        }

        if (
                container.status()
                        == ModuleStatus.ERROR
        ) {
            cleanup(container);

            container.status(
                    ModuleStatus.DISABLED
            );
        }

        try {
            if (
                    !enableRequiredDependencies(
                            container,
                            chain
                    )
            ) {
                container.status(
                        ModuleStatus.ERROR
                );

                return false;
            }

            enableSoftDependencies(
                    container,
                    chain
            );

            container.status(
                    ModuleStatus.ENABLING
            );

            container.module()
                    .configs()
                    .main();

            try {
                container.module()
                        .onEnable();

            } catch (Exception exception) {
                handleEnableFailure(
                        container,
                        exception
                );

                return false;
            }

            container.status(
                    ModuleStatus.ENABLED
            );

            core.eventBus().publish(
                    new ModuleEnabledEvent(
                            container.module()
                    )
            );

            container.module()
                    .logger()
                    .info(
                            "Módulo activado."
                    );

            return true;

        } finally {
            chain.remove(id);
        }
    }

    private boolean enableRequiredDependencies(
            ModuleContainer container,
            Set<String> chain
    ) {
        for (
                String dependencyId :
                container.descriptor()
                        .dependencies()
        ) {
            ModuleContainer dependency =
                    modules.get(
                            dependencyId
                    );

            if (dependency == null) {
                container.module()
                        .logger()
                        .error(
                                "Falta la dependencia requerida "
                                        + dependencyId
                                        + "."
                        );

                return false;
            }

            if (
                    !enable(
                            dependencyId,
                            chain
                    )
            ) {
                container.module()
                        .logger()
                        .error(
                                "No se pudo activar la dependencia requerida "
                                        + dependencyId
                                        + "."
                        );

                return false;
            }
        }

        return true;
    }

    private void enableSoftDependencies(
            ModuleContainer container,
            Set<String> chain
    ) {
        for (
                String dependencyId :
                container.descriptor()
                        .softDependencies()
        ) {
            ModuleContainer dependency =
                    modules.get(
                            dependencyId
                    );

            if (dependency == null) {
                continue;
            }

            if (
                    dependency.status()
                            == ModuleStatus.ENABLED
            ) {
                continue;
            }

            if (
                    chain.contains(
                            dependencyId
                    )
                            || dependency.status()
                            == ModuleStatus.ENABLING
            ) {
                container.module()
                        .logger()
                        .debug(
                                "Soft-dependency circular ignorada durante enable: "
                                        + dependencyId
                        );

                continue;
            }

            if (
                    !enable(
                            dependencyId,
                            chain
                    )
            ) {
                container.module()
                        .logger()
                        .warn(
                                "La soft-dependency "
                                        + dependencyId
                                        + " no pudo activarse. El módulo continuará."
                        );
            }
        }
    }

    private void handleEnableFailure(
            ModuleContainer container,
            Exception exception
    ) {
        container.module()
                .logger()
                .error(
                        "Error durante onEnable(). Ejecutando rollback.",
                        exception
                );

        try {
            container.module()
                    .onDisable();

        } catch (Exception disableException) {
            exception.addSuppressed(
                    disableException
            );

            container.module()
                    .logger()
                    .error(
                            "También falló onDisable() durante el rollback.",
                            disableException
                    );
        }

        cleanup(container);

        container.status(
                ModuleStatus.ERROR
        );
    }

    private boolean disableContainer(
            ModuleContainer container,
            boolean publishEvent
    ) {
        if (
                container.status()
                        == ModuleStatus.DISABLED
        ) {
            cleanup(container);
            return true;
        }

        if (
                container.status()
                        == ModuleStatus.DISABLING
        ) {
            return true;
        }

        if (
                container.status()
                        == ModuleStatus.ERROR
        ) {
            cleanup(container);

            container.status(
                    ModuleStatus.DISABLED
            );

            return true;
        }

        boolean success = true;

        container.status(
                ModuleStatus.DISABLING
        );

        try {
            container.module()
                    .onDisable();

        } catch (Exception exception) {
            success = false;

            container.module()
                    .logger()
                    .error(
                            "Error durante onDisable().",
                            exception
                    );

        } finally {
            cleanup(container);

            container.status(
                    ModuleStatus.DISABLED
            );
        }

        if (publishEvent) {
            core.eventBus().publish(
                    new ModuleDisabledEvent(
                            container.module()
                    )
            );
        }

        if (success) {
            container.module()
                    .logger()
                    .info(
                            "Módulo desactivado."
                    );
        }

        return success;
    }

    private void cleanup(
            ModuleContainer container
    ) {
        AriatusModule module =
                container.module();

        cleanupStep(
                module,
                "tasks",
                () -> core.taskManager()
                        .cancelAll(module)
        );

        cleanupStep(
                module,
                "event subscriptions",
                () -> core.eventBus()
                        .unsubscribeAll(module)
        );

        cleanupStep(
                module,
                "listeners",
                () -> core.listenerManager()
                        .unregisterAll(module)
        );

        cleanupStep(
                module,
                "commands",
                () -> core.commandManager()
                        .unregisterAll(module)
        );

        cleanupStep(
                module,
                "managed resources",
                () -> container.context()
                        .closeManagedResources()
        );

        cleanupStep(
                module,
                "services",
                () -> core.services()
                        .unregisterAll(module)
        );

        cleanupStep(
                module,
                "configs",
                () -> core.moduleConfigManager()
                        .unload(module)
        );
    }

    private void cleanupStep(
            AriatusModule module,
            String resource,
            Runnable action
    ) {
        try {
            action.run();

        } catch (Throwable throwable) {
            module.logger()
                    .error(
                            "Error limpiando "
                                    + resource
                                    + ".",
                            throwable
                    );
        }
    }

    private Optional<ModuleContainer> findEnabledRuntimeDependent(
            String moduleId
    ) {
        return modules.values()
                .stream()
                .filter(
                        ModuleContainer::isEnabled
                )
                .filter(container ->
                        !container.descriptor()
                                .id()
                                .equals(
                                        moduleId
                                )
                )
                .filter(container ->
                        container.classLoader()
                                .dependsOn(
                                        moduleId
                                )
                )
                .findFirst();
    }

    private Optional<ModuleContainer> findLoadedRuntimeDependent(
            String moduleId
    ) {
        return modules.values()
                .stream()
                .filter(container ->
                        !container.descriptor()
                                .id()
                                .equals(
                                        moduleId
                                )
                )
                .filter(container ->
                        container.classLoader()
                                .dependsOn(
                                        moduleId
                                )
                )
                .findFirst();
    }

    private void visitRuntimeDependencies(
            String moduleId,
            Set<String> visiting,
            Set<String> visited,
            List<String> order
    ) {
        if (visited.contains(moduleId)) {
            return;
        }

        if (!visiting.add(moduleId)) {
            core.loggerService().warn(
                    "Ciclo detectado en dependencias runtime alrededor de "
                            + moduleId
                            + "."
            );

            return;
        }

        ModuleContainer container =
                modules.get(moduleId);

        if (container != null) {
            for (
                    String dependencyId :
                    container.classLoader()
                            .dependencyIds()
            ) {
                if (
                        modules.containsKey(
                                dependencyId
                        )
                ) {
                    visitRuntimeDependencies(
                            dependencyId,
                            visiting,
                            visited,
                            order
                    );
                }
            }
        }

        visiting.remove(moduleId);

        if (visited.add(moduleId)) {
            order.add(moduleId);
        }
    }

    private String normalizeId(String id) {
        return id.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private void ensurePrimaryThread(
            String action
    ) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "AriatusCore solo puede "
                            + action
                            + " desde el thread principal."
            );
        }
    }
}