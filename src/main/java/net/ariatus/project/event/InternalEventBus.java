package net.ariatus.project.event;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.profiler.ProfilerCategory;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InternalEventBus {

    private static final String CORE_OWNER = "core";

    private final AriatusCore core;

    private final Map<Class<? extends AriatusEvent>, CopyOnWriteArrayList<OwnedListener>> listeners =
            new ConcurrentHashMap<>();

    public InternalEventBus(
            AriatusCore core
    ) {
        this.core =
                Objects.requireNonNull(
                        core,
                        "core"
                );
    }

    public <T extends AriatusEvent> void subscribe(
            Class<T> eventClass,
            AriatusEventListener<T> listener
    ) {
        subscribe(
                CORE_OWNER,
                eventClass,
                listener
        );
    }

    public <T extends AriatusEvent> void subscribe(
            AriatusModule module,
            Class<T> eventClass,
            AriatusEventListener<T> listener
    ) {
        Objects.requireNonNull(
                module,
                "module"
        );

        subscribe(
                module.id(),
                eventClass,
                listener
        );
    }

    public <T extends AriatusEvent> void publish(
            T event
    ) {
        Objects.requireNonNull(
                event,
                "event"
        );

        CopyOnWriteArrayList<OwnedListener> eventListeners =
                listeners.get(
                        event.getClass()
                );

        if (eventListeners == null) {
            return;
        }

        for (
                OwnedListener ownedListener :
                eventListeners
        ) {
            long start =
                    System.nanoTime();

            boolean profile =
                    !CORE_OWNER.equals(
                            ownedListener.ownerId()
                    );

            try {
                @SuppressWarnings("unchecked")
                AriatusEventListener<T> listener =
                        (AriatusEventListener<T>)
                                ownedListener.listener();

                listener.handle(
                        event
                );

            } catch (Exception exception) {
                if (profile) {
                    core.profiler()
                            .error(
                                    ownedListener.ownerId(),
                                    ProfilerCategory.INTERNAL_EVENT
                            );
                }

                core.loggerService()
                        .error(
                                "Error ejecutando evento interno "
                                        + event.getClass()
                                        .getSimpleName()
                                        + " para "
                                        + ownedListener.ownerId()
                                        + ".",
                                exception
                        );

            } finally {
                if (profile) {
                    core.profiler()
                            .record(
                                    ownedListener.ownerId(),
                                    ProfilerCategory.INTERNAL_EVENT,
                                    System.nanoTime()
                                            - start,
                                    Bukkit.isPrimaryThread()
                            );
                }
            }
        }
    }

    public void unsubscribeAll(
            AriatusModule module
    ) {
        Objects.requireNonNull(
                module,
                "module"
        );

        String ownerId =
                module.id();

        listeners.values()
                .forEach(eventListeners ->
                        eventListeners.removeIf(listener ->
                                listener.ownerId()
                                        .equals(ownerId)
                        )
                );

        listeners.entrySet()
                .removeIf(entry ->
                        entry.getValue()
                                .isEmpty()
                );
    }

    public void clear() {
        listeners.clear();
    }

    private <T extends AriatusEvent> void subscribe(
            String ownerId,
            Class<T> eventClass,
            AriatusEventListener<T> listener
    ) {
        Objects.requireNonNull(
                eventClass,
                "eventClass"
        );

        Objects.requireNonNull(
                listener,
                "listener"
        );

        listeners.computeIfAbsent(
                        eventClass,
                        ignored ->
                                new CopyOnWriteArrayList<>()
                )
                .add(
                        new OwnedListener(
                                ownerId,
                                listener
                        )
                );
    }

    private record OwnedListener(
            String ownerId,
            AriatusEventListener<? extends AriatusEvent> listener
    ) {
    }
}