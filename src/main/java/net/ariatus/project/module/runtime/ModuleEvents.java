package net.ariatus.project.module.runtime;

import net.ariatus.project.event.AriatusEvent;
import net.ariatus.project.event.AriatusEventListener;
import net.ariatus.project.event.InternalEventBus;
import net.ariatus.project.module.AriatusModule;

import java.util.Objects;

public final class ModuleEvents {

    private final AriatusModule module;
    private final InternalEventBus eventBus;

    public ModuleEvents(AriatusModule module, InternalEventBus eventBus) {
        this.module = Objects.requireNonNull(module, "module");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    public <T extends AriatusEvent> void subscribe(Class<T> eventType, AriatusEventListener<T> listener) {
        eventBus.subscribe(module, eventType, listener);
    }

    public <T extends AriatusEvent> void publish(T event) {
        eventBus.publish(event);
    }

    public void unsubscribeAll() {
        eventBus.unsubscribeAll(module);
    }
}