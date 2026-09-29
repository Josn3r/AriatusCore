package net.ariatus.project.command;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class AriatusCommandManager {

    private final AriatusCore core;
    private final CommandMap commandMap;

    private final Map<String, Registration> registrationsByPrimary = new LinkedHashMap<>();
    private final Map<String, Registration> registrationsByLabel = new LinkedHashMap<>();

    public AriatusCommandManager(AriatusCore core) {
        this.core = Objects.requireNonNull(core, "core");
        this.commandMap = Bukkit.getCommandMap();
    }

    public synchronized <T extends AriatusCommandExecutor> T register(AriatusModule module, T executor) {
        ensurePrimaryThread("registrar comandos");

        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(executor, "executor");

        String primary = normalizeLabel(executor.name());

        LinkedHashSet<String> requestedLabels = new LinkedHashSet<>();
        requestedLabels.add(primary);

        for (String alias : executor.aliases()) {
            requestedLabels.add(normalizeLabel(alias));
        }

        validateAvailable(requestedLabels);

        AriatusDynamicCommand dynamicCommand = new AriatusDynamicCommand(
                core,
                module,
                executor
        );

        boolean registered = commandMap.register(
                "ariatus",
                dynamicCommand
        );

        if (!registered) {
            removeFromCommandMap(dynamicCommand);

            throw new IllegalStateException(
                    "Paper no pudo registrar el comando /" + primary + "."
            );
        }

        Set<String> bukkitKeys = findKeys(dynamicCommand);

        Registration registration = new Registration(
                module.id(),
                primary,
                executor,
                dynamicCommand,
                Set.copyOf(requestedLabels),
                bukkitKeys
        );

        registrationsByPrimary.put(
                primary,
                registration
        );

        for (String label : requestedLabels) {
            registrationsByLabel.put(
                    label,
                    registration
            );
        }

        module.logger().debug(
                "Comando registrado: /"
                        + primary
                        + (
                        executor.aliases().isEmpty()
                                ? ""
                                : " " + executor.aliases()
                )
        );

        return executor;
    }

    public synchronized Optional<AriatusCommandExecutor> getCommand(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        Registration registration = registrationsByLabel.get(
                name.toLowerCase(Locale.ROOT)
        );

        return registration == null
                ? Optional.empty()
                : Optional.of(registration.executor());
    }

    public synchronized Collection<AriatusCommandExecutor> getCommands() {
        return registrationsByPrimary.values()
                .stream()
                .map(Registration::executor)
                .toList();
    }

    public synchronized int activeCommands(AriatusModule module) {
        String moduleId = module.id();

        return (int) registrationsByPrimary.values()
                .stream()
                .filter(registration -> registration.moduleId().equals(moduleId))
                .count();
    }

    public synchronized void unregisterAll(AriatusModule module) {
        ensurePrimaryThread("desregistrar comandos");

        String moduleId = Objects.requireNonNull(module, "module").id();

        List<Registration> registrations = registrationsByPrimary.values()
                .stream()
                .filter(registration -> registration.moduleId().equals(moduleId))
                .toList();

        for (Registration registration : registrations) {
            unregister(registration);
        }
    }

    public synchronized void unregisterAll() {
        ensurePrimaryThread("desregistrar comandos");

        List<Registration> registrations = new ArrayList<>(
                registrationsByPrimary.values()
        );

        for (Registration registration : registrations) {
            unregister(registration);
        }

        registrationsByPrimary.clear();
        registrationsByLabel.clear();
    }

    private void unregister(Registration registration) {
        registrationsByPrimary.remove(
                registration.primary()
        );

        for (String label : registration.labels()) {
            registrationsByLabel.remove(
                    label,
                    registration
            );
        }

        removeFromCommandMap(
                registration.command()
        );
    }

    private void removeFromCommandMap(AriatusDynamicCommand command) {
        command.unregister(commandMap);

        commandMap.getKnownCommands()
                .entrySet()
                .removeIf(entry -> entry.getValue() == command);
    }

    private Set<String> findKeys(Command command) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();

        commandMap.getKnownCommands().forEach((key, registeredCommand) -> {
            if (registeredCommand == command) {
                keys.add(key);
            }
        });

        return Set.copyOf(keys);
    }

    private void validateAvailable(Set<String> labels) {
        for (String label : labels) {
            if (registrationsByLabel.containsKey(label)) {
                throw new IllegalStateException(
                        "El comando o alias /" + label + " ya está registrado por otro módulo Ariatus."
                );
            }

            Command existing = commandMap.getCommand(label);

            if (existing != null) {
                throw new IllegalStateException(
                        "El comando o alias /" + label + " ya está registrado en el servidor."
                );
            }
        }
    }

    private String normalizeLabel(String value) {
        String label = Objects.requireNonNull(value, "command label")
                .trim()
                .toLowerCase(Locale.ROOT);

        if (label.isEmpty()) {
            throw new IllegalArgumentException("El nombre del comando no puede estar vacío.");
        }

        if (!label.matches("^[a-z0-9][a-z0-9_-]*$")) {
            throw new IllegalArgumentException(
                    "Nombre de comando inválido: " + value
            );
        }

        return label;
    }

    private void ensurePrimaryThread(String action) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "AriatusCore solo puede " + action + " desde el thread principal."
            );
        }
    }

    private record Registration(
            String moduleId,
            String primary,
            AriatusCommandExecutor executor,
            AriatusDynamicCommand command,
            Set<String> labels,
            Set<String> bukkitKeys
    ) {
    }
}