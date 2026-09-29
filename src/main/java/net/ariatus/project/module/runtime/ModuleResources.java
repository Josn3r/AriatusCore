package net.ariatus.project.module.runtime;

import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.module.resource.ManagedResourceRegistry;
import net.ariatus.project.module.resource.ResourceCloser;

import java.util.Objects;

public final class ModuleResources {

    private final AriatusModule module;
    private final ManagedResourceRegistry registry;

    public ModuleResources(
            AriatusModule module,
            ManagedResourceRegistry registry
    ) {
        this.module = Objects.requireNonNull(
                module,
                "module"
        );

        this.registry = Objects.requireNonNull(
                registry,
                "registry"
        );
    }

    public <T extends AutoCloseable> T manage(T resource) {
        ensureRegistrationAllowed();

        return registry.manage(resource);
    }

    public <T> T manage(
            T resource,
            ResourceCloser<? super T> closer
    ) {
        ensureRegistrationAllowed();

        return registry.manage(
                resource,
                closer
        );
    }

    public void onClose(Runnable action) {
        ensureRegistrationAllowed();

        registry.onClose(action);
    }

    public boolean release(Object resource) {
        return registry.release(resource);
    }

    public int active() {
        return registry.active();
    }

    private void ensureRegistrationAllowed() {
        ModuleStatus status =
                module.status();

        if (
                status != ModuleStatus.ENABLING
                        && status != ModuleStatus.ENABLED
        ) {
            throw new IllegalStateException(
                    "El módulo "
                            + module.id()
                            + " no puede registrar recursos mientras está "
                            + status
                            + "."
            );
        }

        if (registry.isClosing()) {
            throw new IllegalStateException(
                    "Los recursos de "
                            + module.id()
                            + " se están cerrando."
            );
        }
    }
}