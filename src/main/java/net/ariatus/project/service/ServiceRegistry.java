package net.ariatus.project.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServiceRegistry {

    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void register(Class<T> serviceClass, T serviceInstance) {
        services.put(serviceClass, serviceInstance);
    }

    public <T> Optional<T> get(Class<T> serviceClass) {
        Object service = services.get(serviceClass);

        if (service == null) {
            return Optional.empty();
        }

        return Optional.of(serviceClass.cast(service));
    }

    public <T> T require(Class<T> serviceClass) {
        return get(serviceClass).orElseThrow(() ->
                new IllegalStateException("Servicio no registrado: " + serviceClass.getSimpleName())
        );
    }

    public boolean has(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }

    public void clear() {
        services.clear();
    }
}