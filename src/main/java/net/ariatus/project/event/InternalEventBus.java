package net.ariatus.project.event;

import net.ariatus.project.AriatusCore;

import java.util.*;

public class InternalEventBus {

    private final AriatusCore core;
    private final Map<Class<? extends AriatusEvent>, List<AriatusEventListener<? extends AriatusEvent>>> listeners = new HashMap<>();

    public InternalEventBus(AriatusCore core) {
        this.core = core;
    }

    public <T extends AriatusEvent> void subscribe(Class<T> eventClass, AriatusEventListener<T> listener) {
        listeners.computeIfAbsent(eventClass, key -> new ArrayList<>()).add(listener);
    }

    public <T extends AriatusEvent> void publish(T event) {
        List<AriatusEventListener<? extends AriatusEvent>> eventListeners =
                listeners.getOrDefault(event.getClass(), List.of());

        for (AriatusEventListener<? extends AriatusEvent> listener : eventListeners) {
            try {
                @SuppressWarnings("unchecked")
                AriatusEventListener<T> typedListener = (AriatusEventListener<T>) listener;
                typedListener.handle(event);
            } catch (Exception exception) {
                core.loggerService().error("Error ejecutando evento interno " + event.getClass().getSimpleName()
                        + ": " + exception.getMessage());
            }
        }
    }

    public void clear() {
        listeners.clear();
    }
}