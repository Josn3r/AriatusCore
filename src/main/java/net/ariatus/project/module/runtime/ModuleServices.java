package net.ariatus.project.module.runtime;

import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.service.ServiceRegistry;

import java.util.Objects;
import java.util.Optional;

public final class ModuleServices {

    private final AriatusModule module;
    private final ServiceRegistry registry;

    public ModuleServices(AriatusModule module, ServiceRegistry registry) {
        this.module = Objects.requireNonNull(module, "module");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public <T> T provide(Class<T> type, T service) {
        registry.provide(module, type, service);
        return service;
    }

    public <T> Optional<T> find(Class<T> type) {
        return registry.find(type);
    }

    public <T> T require(Class<T> type) {
        return registry.require(type);
    }

    public boolean has(Class<?> type) {
        return registry.has(type);
    }

    public boolean remove(Class<?> type) {
        return registry.unregister(module, type);
    }
}