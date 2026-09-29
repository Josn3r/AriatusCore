package net.ariatus.project.service;

import net.ariatus.project.module.AriatusModule;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ServiceRegistry {

    private static final String CORE_OWNER = "core";

    private final ConcurrentMap<Class<?>, ServiceEntry> services = new ConcurrentHashMap<>();

    public <T> T registerCore(Class<T> type, T service) {
        register(CORE_OWNER, type, service);
        return service;
    }

    public <T> T provide(AriatusModule module, Class<T> type, T service) {
        Objects.requireNonNull(module, "module");

        register(module.id(), type, service);

        return service;
    }

    public <T> Optional<T> find(Class<T> type) {
        Objects.requireNonNull(type, "type");

        ServiceEntry entry = services.get(type);

        if (entry == null) {
            return Optional.empty();
        }

        return Optional.of(
                type.cast(
                        entry.service()
                )
        );
    }

    public <T> Optional<T> get(Class<T> type) {
        return find(type);
    }

    public <T> T require(Class<T> type) {
        return find(type)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Servicio no registrado: "
                                        + type.getName()
                        )
                );
    }

    public boolean has(Class<?> type) {
        return services.containsKey(
                Objects.requireNonNull(
                        type,
                        "type"
                )
        );
    }

    public boolean unregister(
            AriatusModule module,
            Class<?> type
    ) {
        Objects.requireNonNull(
                module,
                "module"
        );

        Objects.requireNonNull(
                type,
                "type"
        );

        ServiceEntry entry =
                services.get(type);

        if (
                entry == null
                        || !entry.ownerId()
                        .equals(module.id())
        ) {
            return false;
        }

        return services.remove(
                type,
                entry
        );
    }

    public void unregisterAll(
            AriatusModule module
    ) {
        Objects.requireNonNull(
                module,
                "module"
        );

        String ownerId =
                module.id();

        services.entrySet()
                .removeIf(entry ->
                        entry.getValue()
                                .ownerId()
                                .equals(ownerId)
                );
    }

    public int countOwned(
            AriatusModule module
    ) {
        Objects.requireNonNull(
                module,
                "module"
        );

        String ownerId =
                module.id();

        return (int) services.values()
                .stream()
                .filter(entry ->
                        entry.ownerId()
                                .equals(ownerId)
                )
                .count();
    }

    public Set<Class<?>> typesOwnedBy(
            AriatusModule module
    ) {
        Objects.requireNonNull(
                module,
                "module"
        );

        String ownerId =
                module.id();

        return services.entrySet()
                .stream()
                .filter(entry ->
                        entry.getValue()
                                .ownerId()
                                .equals(ownerId)
                )
                .map(Map.Entry::getKey)
                .collect(
                        java.util.stream.Collectors.toUnmodifiableSet()
                );
    }

    public Optional<String> owner(
            Class<?> type
    ) {
        ServiceEntry entry =
                services.get(type);

        return entry == null
                ? Optional.empty()
                : Optional.of(
                entry.ownerId()
        );
    }

    public int size() {
        return services.size();
    }

    public Map<Class<?>, Object> snapshot() {
        Map<Class<?>, Object> snapshot =
                new ConcurrentHashMap<>();

        services.forEach(
                (type, entry) ->
                        snapshot.put(
                                type,
                                entry.service()
                        )
        );

        return Map.copyOf(
                snapshot
        );
    }

    public void clear() {
        services.clear();
    }

    private <T> void register(
            String ownerId,
            Class<T> type,
            T service
    ) {
        Objects.requireNonNull(
                ownerId,
                "ownerId"
        );

        Objects.requireNonNull(
                type,
                "type"
        );

        Objects.requireNonNull(
                service,
                "service"
        );

        if (!type.isInstance(service)) {
            throw new IllegalArgumentException(
                    "La instancia "
                            + service.getClass().getName()
                            + " no implementa "
                            + type.getName()
                            + "."
            );
        }

        ServiceEntry newEntry =
                new ServiceEntry(
                        ownerId,
                        service
                );

        ServiceEntry previous =
                services.putIfAbsent(
                        type,
                        newEntry
                );

        if (previous != null) {
            throw new IllegalStateException(
                    "El servicio "
                            + type.getName()
                            + " ya está registrado por "
                            + previous.ownerId()
                            + "."
            );
        }
    }

    private record ServiceEntry(
            String ownerId,
            Object service
    ) {
    }
}