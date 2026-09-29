package net.ariatus.project.module.loader;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.event.ModuleReloadedEvent;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.module.ModuleContainer;
import net.ariatus.project.module.ModuleDescriptor;
import net.ariatus.project.module.ModuleStatus;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;

public final class AriatusModuleLoader {

    private final AriatusCore core;
    private final File modulesFolder;

    private final Map<String, ModuleDescriptor> discoveredModules = new LinkedHashMap<>();
    private final Map<String, ModuleContainer> loadedModules = new LinkedHashMap<>();
    private final Map<String, String> failedModules = new LinkedHashMap<>();

    public AriatusModuleLoader(AriatusCore core) {
        this.core = Objects.requireNonNull(
                core,
                "core"
        );

        this.modulesFolder =
                new File(
                        core.getDataFolder(),
                        "modules"
                );
    }

    public void loadFolder() {
        ensurePrimaryThread(
                "crear la carpeta de módulos"
        );

        if (
                !modulesFolder.exists()
                        && modulesFolder.mkdirs()
        ) {
            core.loggerService().info(
                    "Carpeta de módulos creada: "
                            + modulesFolder.getPath()
            );
        }
    }

    public void discoverModules() {
        ensurePrimaryThread(
                "buscar módulos"
        );

        discoveredModules.clear();
        failedModules.clear();

        loadFolder();

        File[] files =
                modulesFolder.listFiles(
                        (directory, name) ->
                                name.toLowerCase(
                                                Locale.ROOT
                                        )
                                        .endsWith(".jar")
                );

        if (
                files == null
                        || files.length == 0
        ) {
            core.loggerService().warn(
                    "No se encontraron módulos en /AriatusCore/modules/."
            );

            return;
        }

        Arrays.sort(
                files,
                Comparator.comparing(
                        File::getName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        Set<String> duplicateIds =
                new LinkedHashSet<>();

        for (File file : files) {
            discoverModule(
                    file,
                    duplicateIds
            );
        }

        for (String duplicateId : duplicateIds) {
            discoveredModules.remove(
                    duplicateId
            );
        }

        validateDiscoveredModules();

        core.loggerService().info(
                "Escaneo completado. Detectados: "
                        + discoveredModules.size()
                        + ", fallidos: "
                        + failedModules.size()
                        + "."
        );
    }

    public void loadModules() {
        ensurePrimaryThread(
                "cargar módulos"
        );

        for (
                String id :
                List.copyOf(
                        discoveredModules.keySet()
                )
        ) {
            loadModuleById(id);
        }
    }

    public boolean loadModuleById(
            String id
    ) {
        ensurePrimaryThread(
                "cargar módulos"
        );

        if (id == null) {
            return false;
        }

        return loadModuleById(
                normalizeId(id),
                new LinkedHashSet<>()
        );
    }

    public boolean unloadModule(
            String id
    ) {
        ensurePrimaryThread(
                "descargar módulos"
        );

        if (id == null) {
            return false;
        }

        String moduleId =
                normalizeId(id);

        ModuleContainer container =
                loadedModules.get(
                        moduleId
                );

        if (container == null) {
            return false;
        }

        if (
                !core.moduleManager()
                        .unregister(moduleId)
        ) {
            return false;
        }

        loadedModules.remove(
                moduleId
        );

        closeQuietly(
                container.classLoader()
        );

        failedModules.remove(
                moduleId
        );

        core.loggerService().info(
                "Módulo descargado: "
                        + moduleId
        );

        return true;
    }

    public boolean reloadModule(
            String id
    ) {
        ensurePrimaryThread(
                "recargar módulos"
        );

        if (id == null) {
            return false;
        }

        String targetId =
                normalizeId(id);

        ModuleContainer target =
                loadedModules.get(
                        targetId
                );

        if (target == null) {
            core.loggerService().warn(
                    "No se puede recargar "
                            + targetId
                            + ": no está cargado."
            );

            return false;
        }

        Set<String> affected =
                core.moduleManager()
                        .runtimeDependentsClosure(
                                targetId
                        );

        if (affected.isEmpty()) {
            return false;
        }

        Map<String, Boolean> enabledBefore =
                new LinkedHashMap<>();

        Map<String, File> files =
                new LinkedHashMap<>();

        for (String moduleId : affected) {
            ModuleContainer container =
                    loadedModules.get(
                            moduleId
                    );

            if (container == null) {
                continue;
            }

            enabledBefore.put(
                    moduleId,
                    container.status()
                            == ModuleStatus.ENABLED
            );

            files.put(
                    moduleId,
                    container.descriptor()
                            .file()
            );
        }

        Map<String, ModuleDescriptor> freshDescriptors;

        try {
            freshDescriptors =
                    preflightReload(
                            affected,
                            files
                    );

        } catch (Exception exception) {
            core.loggerService().error(
                    "Se canceló el reload de "
                            + targetId
                            + " durante la validación previa.",
                    exception
            );

            return false;
        }

        List<String> shutdownOrder =
                core.moduleManager()
                        .shutdownOrderIds()
                        .stream()
                        .filter(
                                affected::contains
                        )
                        .toList();

        List<String> loadOrder =
                new ArrayList<>(
                        shutdownOrder
                );

        Collections.reverse(
                loadOrder
        );

        core.loggerService().info(
                "Reload de "
                        + targetId
                        + " afecta a "
                        + affected.size()
                        + " módulo(s): "
                        + String.join(
                                ", ",
                                affected
                        )
        );

        for (String moduleId : shutdownOrder) {
            ModuleContainer container =
                    loadedModules.get(
                            moduleId
                    );

            if (container == null) {
                continue;
            }

            if (
                    !core.moduleManager()
                            .unregister(
                                    moduleId
                            )
            ) {
                core.loggerService().error(
                        "No se pudo descargar "
                                + moduleId
                                + " durante el reload de "
                                + targetId
                                + "."
                );

                return false;
            }

            loadedModules.remove(
                    moduleId
            );

            closeQuietly(
                    container.classLoader()
            );
        }

        for (
                Map.Entry<String, ModuleDescriptor> entry :
                freshDescriptors.entrySet()
        ) {
            discoveredModules.put(
                    entry.getKey(),
                    entry.getValue()
            );

            failedModules.remove(
                    entry.getKey()
            );
        }

        boolean loadSuccess = true;

        for (String moduleId : loadOrder) {
            if (
                    !loadModuleById(
                            moduleId
                    )
            ) {
                loadSuccess = false;

                core.loggerService().error(
                        "No se pudo volver a cargar "
                                + moduleId
                                + " durante el reload de "
                                + targetId
                                + "."
                );
            }
        }

        boolean enableSuccess = true;

        for (
                String moduleId :
                core.moduleManager()
                        .startupOrderIds()
        ) {
            if (
                    !affected.contains(moduleId)
                            || !enabledBefore.getOrDefault(
                            moduleId,
                            false
                    )
            ) {
                continue;
            }

            if (
                    !core.moduleManager()
                            .enable(moduleId)
            ) {
                enableSuccess = false;

                core.loggerService().error(
                        "No se pudo reactivar "
                                + moduleId
                                + " después del reload de "
                                + targetId
                                + "."
                );
            }
        }

        ModuleContainer reloadedTarget =
                loadedModules.get(
                        targetId
                );

        boolean allAffectedLoaded =
                affected.stream()
                        .allMatch(
                                loadedModules::containsKey
                        );

        boolean success =
                loadSuccess
                        && enableSuccess
                        && reloadedTarget != null
                        && allAffectedLoaded;

        if (success) {
            core.eventBus().publish(
                    new ModuleReloadedEvent(
                            reloadedTarget.module()
                    )
            );

            core.loggerService().info(
                    "Reload completado: "
                            + targetId
            );

        } else {
            core.loggerService().error(
                    "El reload de "
                            + targetId
                            + " terminó parcialmente."
            );
        }

        return success;
    }

    public void unloadAll() {
        ensurePrimaryThread(
                "descargar módulos"
        );

        List<String> shutdownOrder =
                core.moduleManager()
                        .shutdownOrderIds();

        for (String id : shutdownOrder) {
            ModuleContainer container =
                    loadedModules.get(id);

            if (container == null) {
                continue;
            }

            if (
                    !core.moduleManager()
                            .unregister(id)
            ) {
                core.loggerService().error(
                        "No se pudo descargar correctamente el módulo "
                                + id
                                + "."
                );

                continue;
            }

            loadedModules.remove(id);

            closeQuietly(
                    container.classLoader()
            );
        }

        if (!loadedModules.isEmpty()) {
            core.loggerService().warn(
                    "Quedaron "
                            + loadedModules.size()
                            + " módulo(s) cargados después de unloadAll()."
            );
        }
    }

    public Optional<ModuleDescriptor> discoveredModule(
            String id
    ) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                discoveredModules.get(
                        normalizeId(id)
                )
        );
    }

    public boolean isDiscovered(
            String id
    ) {
        return discoveredModule(id)
                .isPresent();
    }

    public List<ModuleDescriptor> discoveredModules() {
        return List.copyOf(
                discoveredModules.values()
        );
    }

    public Map<String, ModuleContainer> loadedModules() {
        return Collections.unmodifiableMap(
                loadedModules
        );
    }

    public Map<String, String> failedModules() {
        return Collections.unmodifiableMap(
                failedModules
        );
    }

    private void discoverModule(
            File file,
            Set<String> duplicateIds
    ) {
        try {
            ModuleDescriptor descriptor =
                    readDescriptor(file);

            if (
                    discoveredModules.containsKey(
                            descriptor.id()
                    )
            ) {
                duplicateIds.add(
                        descriptor.id()
                );

                failedModules.put(
                        descriptor.id(),
                        "ID duplicado entre múltiples JARs."
                );

                core.loggerService().error(
                        "ID de módulo duplicado: "
                                + descriptor.id()
                                + "."
                );

                return;
            }

            discoveredModules.put(
                    descriptor.id(),
                    descriptor
            );

            core.loggerService().info(
                    "Módulo detectado: "
                            + descriptor.name()
                            + " v"
                            + descriptor.version()
                            + " ("
                            + descriptor.id()
                            + ")"
            );

        } catch (Exception exception) {
            failedModules.put(
                    file.getName(),
                    safeMessage(
                            exception
                    )
            );

            core.loggerService().error(
                    "Error leyendo módulo "
                            + file.getName()
                            + ".",
                    exception
            );
        }
    }

    private void validateDiscoveredModules() {
        for (
                ModuleDescriptor descriptor :
                discoveredModules.values()
        ) {
            try {
                validateRequiredNode(
                        descriptor.id(),
                        discoveredModules,
                        new LinkedHashSet<>(),
                        new LinkedHashSet<>()
                );

            } catch (Exception exception) {
                failedModules.put(
                        descriptor.id(),
                        safeMessage(
                                exception
                        )
                );

                core.loggerService().warn(
                        "Módulo "
                                + descriptor.id()
                                + " tiene un problema de dependencias: "
                                + safeMessage(exception)
                );
            }
        }
    }

    private boolean loadModuleById(
            String id,
            Set<String> visiting
    ) {
        if (
                loadedModules.containsKey(id)
        ) {
            return true;
        }

        ModuleDescriptor descriptor =
                discoveredModules.get(id);

        if (descriptor == null) {
            failedModules.put(
                    id,
                    "Descriptor no encontrado."
            );

            return false;
        }

        if (!visiting.add(id)) {
            String cycle =
                    String.join(
                            " -> ",
                            visiting
                    )
                            + " -> "
                            + id;

            failedModules.put(
                    id,
                    "Dependencia circular: "
                            + cycle
            );

            core.loggerService().error(
                    "Dependencia circular detectada: "
                            + cycle
            );

            return false;
        }

        try {
            for (
                    String dependencyId :
                    descriptor.dependencies()
            ) {
                if (
                        !loadedModules.containsKey(
                                dependencyId
                        )
                                && !discoveredModules.containsKey(
                                dependencyId
                        )
                ) {
                    failedModules.put(
                            id,
                            "Dependencia faltante: "
                                    + dependencyId
                    );

                    core.loggerService().error(
                            "Dependencia faltante para "
                                    + id
                                    + ": "
                                    + dependencyId
                    );

                    return false;
                }

                if (
                        !loadModuleById(
                                dependencyId,
                                visiting
                        )
                ) {
                    failedModules.put(
                            id,
                            "No se pudo cargar dependencia: "
                                    + dependencyId
                    );

                    return false;
                }
            }

            loadAvailableSoftDependencies(
                    descriptor,
                    visiting
            );

            return loadModule(
                    descriptor
            );

        } finally {
            visiting.remove(id);
        }
    }

    private void loadAvailableSoftDependencies(
            ModuleDescriptor descriptor,
            Set<String> visiting
    ) {
        for (
                String dependencyId :
                descriptor.softDependencies()
        ) {
            if (
                    loadedModules.containsKey(
                            dependencyId
                    )
            ) {
                continue;
            }

            if (
                    !discoveredModules.containsKey(
                            dependencyId
                    )
            ) {
                continue;
            }

            if (
                    visiting.contains(
                            dependencyId
                    )
            ) {
                core.loggerService().debug(
                        "Soft-dependency circular ignorada: "
                                + descriptor.id()
                                + " -> "
                                + dependencyId
                );

                continue;
            }

            if (
                    !loadModuleById(
                            dependencyId,
                            visiting
                    )
            ) {
                core.loggerService().warn(
                        "Soft-dependency "
                                + dependencyId
                                + " no pudo cargarse para "
                                + descriptor.id()
                                + "."
                );
            }
        }
    }

    private boolean loadModule(
            ModuleDescriptor descriptor
    ) {
        if (
                loadedModules.containsKey(
                        descriptor.id()
                )
        ) {
            return true;
        }

        AriatusModuleClassLoader classLoader =
                null;

        try {
            URL url =
                    descriptor.file()
                            .toURI()
                            .toURL();

            classLoader =
                    new AriatusModuleClassLoader(
                            new URL[]{url},
                            core.getClass()
                                    .getClassLoader()
                    );

            wireDependencies(
                    descriptor,
                    classLoader
            );

            Class<?> mainClass =
                    classLoader.loadClass(
                            descriptor.main()
                    );

            if (
                    !AriatusModule.class
                            .isAssignableFrom(
                                    mainClass
                            )
            ) {
                throw new IllegalArgumentException(
                        "La clase main "
                                + descriptor.main()
                                + " no extiende AriatusModule."
                );
            }

            AriatusModule module =
                    (AriatusModule) mainClass
                            .getDeclaredConstructor()
                            .newInstance();

            ModuleContainer container =
                    new ModuleContainer(
                            core,
                            descriptor,
                            module,
                            classLoader
                    );

            core.moduleManager()
                    .register(
                            container
                    );

            loadedModules.put(
                    descriptor.id(),
                    container
            );

            failedModules.remove(
                    descriptor.id()
            );

            core.loggerService().info(
                    "Módulo cargado: "
                            + descriptor.name()
                            + " v"
                            + descriptor.version()
            );

            return true;

        } catch (Exception exception) {
            closeQuietly(
                    classLoader
            );

            failedModules.put(
                    descriptor.id(),
                    safeMessage(
                            exception
                    )
            );

            core.loggerService().error(
                    "No se pudo cargar módulo "
                            + descriptor.id()
                            + ".",
                    exception
            );

            return false;
        }
    }

    private void wireDependencies(
            ModuleDescriptor descriptor,
            AriatusModuleClassLoader classLoader
    ) {
        for (
                String dependencyId :
                descriptor.dependencies()
        ) {
            ModuleContainer dependency =
                    loadedModules.get(
                            dependencyId
                    );

            if (dependency == null) {
                throw new IllegalStateException(
                        "Dependencia requerida no cargada: "
                                + dependencyId
                );
            }

            classLoader.addDependency(
                    dependencyId,
                    dependency.classLoader()
            );
        }

        for (
                String dependencyId :
                descriptor.softDependencies()
        ) {
            ModuleContainer dependency =
                    loadedModules.get(
                            dependencyId
                    );

            if (dependency == null) {
                continue;
            }

            classLoader.addDependency(
                    dependencyId,
                    dependency.classLoader()
            );
        }
    }

    private Map<String, ModuleDescriptor> preflightReload(
            Set<String> affected,
            Map<String, File> files
    ) throws Exception {

        Map<String, ModuleDescriptor> fresh =
                new LinkedHashMap<>();

        for (String id : affected) {
            File file =
                    files.get(id);

            if (
                    file == null
                            || !file.isFile()
            ) {
                throw new IllegalStateException(
                        "El JAR de "
                                + id
                                + " ya no existe."
                );
            }

            ModuleDescriptor descriptor =
                    readDescriptor(file);

            if (
                    !descriptor.id()
                            .equals(id)
            ) {
                throw new IllegalStateException(
                        "El módulo "
                                + id
                                + " cambió su ID a "
                                + descriptor.id()
                                + ". Usa unload + scan + load."
                );
            }

            fresh.put(
                    id,
                    descriptor
            );
        }

        Map<String, ModuleDescriptor> futureGraph =
                new LinkedHashMap<>(
                        discoveredModules
                );

        for (
                ModuleContainer loaded :
                loadedModules.values()
        ) {
            futureGraph.put(
                    loaded.descriptor().id(),
                    loaded.descriptor()
            );
        }

        futureGraph.putAll(
                fresh
        );

        for (String root : affected) {
            validateRequiredNode(
                    root,
                    futureGraph,
                    new LinkedHashSet<>(),
                    new LinkedHashSet<>()
            );
        }

        return fresh;
    }

    private void validateRequiredNode(
            String id,
            Map<String, ModuleDescriptor> descriptors,
            Set<String> visiting,
            Set<String> visited
    ) {
        if (visited.contains(id)) {
            return;
        }

        ModuleDescriptor descriptor =
                descriptors.get(id);

        if (descriptor == null) {
            throw new IllegalStateException(
                    "Descriptor requerido no encontrado: "
                            + id
            );
        }

        if (!visiting.add(id)) {
            throw new IllegalStateException(
                    "Dependencia circular detectada: "
                            + String.join(
                            " -> ",
                            visiting
                    )
                            + " -> "
                            + id
            );
        }

        for (
                String dependencyId :
                descriptor.dependencies()
        ) {
            if (
                    !descriptors.containsKey(
                            dependencyId
                    )
            ) {
                throw new IllegalStateException(
                        "El módulo "
                                + id
                                + " requiere "
                                + dependencyId
                                + ", pero no existe."
                );
            }

            validateRequiredNode(
                    dependencyId,
                    descriptors,
                    visiting,
                    visited
            );
        }

        visiting.remove(id);
        visited.add(id);
    }

    private ModuleDescriptor readDescriptor(
            File file
    ) throws Exception {

        try (
                JarFile jarFile =
                        new JarFile(file)
        ) {
            var entry =
                    jarFile.getJarEntry(
                            "ariatus-module.yml"
                    );

            if (entry == null) {
                throw new IllegalArgumentException(
                        "No contiene ariatus-module.yml."
                );
            }

            try (
                    InputStream inputStream =
                            jarFile.getInputStream(
                                    entry
                            );

                    InputStreamReader reader =
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8
                            )
            ) {
                YamlConfiguration yaml =
                        YamlConfiguration.loadConfiguration(
                                reader
                        );

                return new ModuleDescriptor(
                        yaml.getString("id"),
                        yaml.getString("name"),
                        yaml.getString("main"),
                        yaml.getString(
                                "version",
                                "unknown"
                        ),
                        yaml.getStringList(
                                "dependencies"
                        ),
                        yaml.getStringList(
                                "soft-dependencies"
                        ),
                        file
                );
            }
        }
    }

    private void closeQuietly(
            AriatusModuleClassLoader classLoader
    ) {
        if (classLoader == null) {
            return;
        }

        try {
            classLoader.close();

        } catch (Exception exception) {
            core.loggerService().warn(
                    "No se pudo cerrar un ClassLoader de módulo: "
                            + safeMessage(
                            exception
                    )
            );
        }
    }

    private String safeMessage(
            Exception exception
    ) {
        return exception.getMessage() == null
                ? exception.getClass()
                .getSimpleName()
                : exception.getMessage();
    }

    private String normalizeId(
            String id
    ) {
        return id.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private void ensurePrimaryThread(
            String action
    ) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "AriatusCore solo puede "
                            + action
                            + " desde el thread principal."
            );
        }
    }
}