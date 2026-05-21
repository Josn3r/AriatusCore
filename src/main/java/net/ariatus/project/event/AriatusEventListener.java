package net.ariatus.project.event;

@FunctionalInterface
public interface AriatusEventListener<T extends AriatusEvent> {

    void handle(T event);
}