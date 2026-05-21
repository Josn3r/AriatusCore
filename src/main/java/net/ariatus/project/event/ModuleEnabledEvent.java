package net.ariatus.project.event;

import net.ariatus.project.event.AriatusEvent;
import net.ariatus.project.module.AriatusModule;

public record ModuleEnabledEvent(AriatusModule module) implements AriatusEvent {
}