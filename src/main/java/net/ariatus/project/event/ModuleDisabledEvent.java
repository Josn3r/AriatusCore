package net.ariatus.project.event;

import net.ariatus.project.event.AriatusEvent;
import net.ariatus.project.module.AriatusModule;

public record ModuleDisabledEvent(AriatusModule module) implements AriatusEvent {
}