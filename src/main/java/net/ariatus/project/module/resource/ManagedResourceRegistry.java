package net.ariatus.project.module.resource;

import net.ariatus.project.module.runtime.ModuleLogger;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

public final class ManagedResourceRegistry {

    private final Object lock = new Object();

    private final Deque<ManagedResource> resources = new ArrayDeque<>();
    private final Set<Object> trackedResources = Collections.newSetFromMap(new IdentityHashMap<>());

    private boolean closing;

    public <T extends AutoCloseable> T manage(T resource) {
        Objects.requireNonNull(resource, "resource");

        return manage(
                resource,
                AutoCloseable::close
        );
    }

    public <T> T manage(T resource, ResourceCloser<? super T> closer) {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(closer, "closer");

        synchronized (lock) {
            ensureNotClosing();

            if (!trackedResources.add(resource)) {
                throw new IllegalStateException(
                        "El recurso "
                                + resource.getClass().getName()
                                + " ya está siendo gestionado."
                );
            }

            resources.addFirst(
                    new ManagedResource(
                            resource,
                            resource.getClass().getName(),
                            () -> closer.close(resource)
                    )
            );
        }

        return resource;
    }

    public void onClose(Runnable action) {
        Objects.requireNonNull(action, "action");

        synchronized (lock) {
            ensureNotClosing();

            resources.addFirst(
                    new ManagedResource(
                            null,
                            "close callback",
                            action::run
                    )
            );
        }
    }

    public boolean release(Object resource) {
        Objects.requireNonNull(resource, "resource");

        synchronized (lock) {
            if (!trackedResources.remove(resource)) {
                return false;
            }

            return resources.removeIf(entry ->
                    entry.resource() == resource
            );
        }
    }

    public int active() {
        synchronized (lock) {
            return resources.size();
        }
    }

    public boolean isClosing() {
        synchronized (lock) {
            return closing;
        }
    }

    public void closeAll(ModuleLogger logger) {
        Objects.requireNonNull(logger, "logger");

        Deque<ManagedResource> resourcesToClose;

        synchronized (lock) {
            if (closing) {
                return;
            }

            closing = true;

            resourcesToClose = new ArrayDeque<>(resources);

            resources.clear();
            trackedResources.clear();
        }

        try {
            while (!resourcesToClose.isEmpty()) {
                ManagedResource resource =
                        resourcesToClose.removeFirst();

                try {
                    resource.closeable().close();

                } catch (Throwable throwable) {
                    logger.error(
                            "Error cerrando recurso gestionado "
                                    + resource.description()
                                    + ".",
                            throwable
                    );
                }
            }

        } finally {
            synchronized (lock) {
                closing = false;
            }
        }
    }

    private void ensureNotClosing() {
        if (closing) {
            throw new IllegalStateException(
                    "Los recursos del módulo se están cerrando."
            );
        }
    }

    private record ManagedResource(
            Object resource,
            String description,
            AutoCloseable closeable
    ) {
    }
}