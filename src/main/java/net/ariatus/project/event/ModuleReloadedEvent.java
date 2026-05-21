package net.ariatus.project.event;

import net.ariatus.project.event.AriatusEvent;
import net.ariatus.project.module.AriatusModule;

public record ModuleReloadedEvent(AriatusModule module) implements AriatusEvent {
}