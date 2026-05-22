package net.ariatus.project.module.loader;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class AriatusModuleLoader {

    private static final Pattern MODULE_ID_PATTERN =
            Pattern.compile("^[a-z0-9-_]+$");

    private final AriatusCore core;
    private final File modulesFolder;

    private final List<ModuleDescriptor> discoveredModules = new ArrayList<>();
    private final Map<String, LoadedModule> loadedModules = new LinkedHashMap<>();
    private final Map<String, String> failedModules = new LinkedHashMap<>();

    public AriatusModuleLoader(AriatusCore core) {
        this.core = core;
        this.modulesFolder = new File(core.getDataFolder(), "modules");
    }

    public void loadFolder() {
        if (!modulesFolder.exists() && modulesFolder.mkdirs()) {
            core.loggerService().info("Carpeta de módulos creada: " + modulesFolder.getPath());
        }
    }

    public void discoverModules() {
        discoveredModules.clear();
        failedModules.clear();
        loadFolder();

        File[] files = modulesFolder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (files == null || files.length == 0) {
            core.loggerService().warn("No se encontraron módulos externos en /AriatusCore/modules/");
            return;
        }

        for (File file : files) {
            discoverModule(file);
        }
    }

    private void discoverModule(File file) {
        try (JarFile jarFile = new JarFile(file)) {
            var entry = jarFile.getJarEntry("ariatus-module.yml");

            if (entry == null) {
                core.loggerService().warn("El archivo " + file.getName() + " no contiene ariatus-module.yml");
                return;
            }

            try (InputStream inputStream = jarFile.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(inputStream)) {

                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);

                String id = yaml.getString("id");
                String name = yaml.getString("name");
                String main = yaml.getString("main");
                String version = yaml.getString("version", "unknown");
                List<String> dependencies = yaml.getStringList("dependencies");

                if (id == null || !MODULE_ID_PATTERN.matcher(id).matches()) {
                    failedModules.put(file.getName(), "ID inválido. Solo se permite: a-z, 0-9, -, _");
                    core.loggerService().error("ID inválido en módulo " + file.getName());
                    return;
                }

                id = id.toLowerCase();

                if (name == null || main == null) {
                    failedModules.put(id, "Faltan campos obligatorios: name o main");
                    core.loggerService().warn("Módulo inválido en " + file.getName() + ": faltan name o main.");
                    return;
                }

                String finalId = id;
                boolean duplicate = discoveredModules.stream()
                        .anyMatch(module -> module.id().equalsIgnoreCase(finalId));

                if (duplicate || loadedModules.containsKey(id)) {
                    failedModules.put(id, "ID duplicado");
                    core.loggerService().error("ID duplicado detectado: " + id);
                    return;
                }

                List<String> normalizedDependencies = dependencies.stream()
                        .map(String::toLowerCase)
                        .toList();

                ModuleDescriptor descriptor = new ModuleDescriptor(
                        id,
                        name,
                        main,
                        version,
                        normalizedDependencies,
                        file
                );

                discoveredModules.add(descriptor);

                core.loggerService().info("Módulo externo detectado: " + name + " v" + version + " (" + id + ")");
            }

        } catch (Exception exception) {
            failedModules.put(file.getName(), exception.getMessage());
            core.loggerService().error("Error leyendo módulo " + file.getName() + ": " + exception.getMessage());
        }
    }

    public void loadModules() {
        List<ModuleDescriptor> remaining = new ArrayList<>(discoveredModules);

        boolean progress;

        do {
            progress = false;

            Iterator<ModuleDescriptor> iterator = remaining.iterator();

            while (iterator.hasNext()) {
                ModuleDescriptor descriptor = iterator.next();

                boolean dependenciesLoaded = descriptor.dependencies().stream()
                        .allMatch(loadedModules::containsKey);

                if (!dependenciesLoaded) {
                    continue;
                }

                boolean loaded = loadModule(descriptor);

                if (loaded) {
                    iterator.remove();
                    progress = true;
                } else {
                    iterator.remove();
                }
            }

        } while (progress);

        for (ModuleDescriptor descriptor : remaining) {
            failedModules.put(descriptor.id(), "No se pudieron resolver dependencias: " + descriptor.dependencies());
            core.loggerService().error("No se pudo resolver dependencias para: " + descriptor.id());
        }
    }

    public boolean loadModuleById(String id) {
        String moduleId = id.toLowerCase();

        Optional<ModuleDescriptor> descriptor = discoveredModules.stream()
                .filter(module -> module.id().equalsIgnoreCase(moduleId))
                .findFirst();

        if (descriptor.isEmpty()) {
            failedModules.put(moduleId, "Descriptor no encontrado. Ejecuta /ariatus scanmodules");
            return false;
        }

        for (String dependency : descriptor.get().dependencies()) {
            if (!loadedModules.containsKey(dependency)) {
                boolean dependencyLoaded = loadModuleById(dependency);

                if (!dependencyLoaded) {
                    failedModules.put(moduleId, "No se pudo cargar dependencia: " + dependency);
                    return false;
                }
            }
        }

        return loadModule(descriptor.get());
    }

    private boolean loadModule(ModuleDescriptor descriptor) {
        if (loadedModules.containsKey(descriptor.id())) {
            core.loggerService().warn("El módulo " + descriptor.id() + " ya está cargado.");
            return true;
        }

        for (String dependency : descriptor.dependencies()) {
            boolean exists = discoveredModules.stream()
                    .anyMatch(found -> found.id().equalsIgnoreCase(dependency));

            if (!exists) {
                failedModules.put(descriptor.id(), "Dependencia faltante: " + dependency);
                core.loggerService().error("Dependencia faltante para " + descriptor.id() + ": " + dependency);
                return false;
            }
        }

        try {
            URL url = descriptor.file().toURI().toURL();

            AriatusModuleClassLoader classLoader =
                    new AriatusModuleClassLoader(
                            new URL[]{url},
                            core.getClass().getClassLoader()
                    );

            Class<?> clazz = classLoader.loadClass(descriptor.main());

            if (!ExternalAriatusModule.class.isAssignableFrom(clazz)) {
                failedModules.put(descriptor.id(), "La clase main no extiende ExternalAriatusModule");
                core.loggerService().error("El módulo " + descriptor.id() + " no extiende ExternalAriatusModule.");
                classLoader.close();
                return false;
            }

            ExternalAriatusModule module =
                    (ExternalAriatusModule) clazz.getDeclaredConstructor().newInstance();

            module.initialize(core);

            loadedModules.put(
                    descriptor.id(),
                    new LoadedModule(descriptor, module, classLoader)
            );

            core.moduleManager().register(module);

            failedModules.remove(descriptor.id());

            core.loggerService().info("Módulo externo cargado: " + descriptor.name());
            return true;

        } catch (Exception exception) {
            failedModules.put(descriptor.id(), exception.getMessage());
            core.loggerService().error("No se pudo cargar módulo "
                    + descriptor.id() + ": " + exception.getMessage());
            return false;
        }
    }

    public boolean unloadModule(String id) {
        String moduleId = id.toLowerCase();
        LoadedModule loadedModule = loadedModules.get(moduleId);

        if (loadedModule == null) {
            failedModules.put(moduleId, "No está cargado");
            return false;
        }

        try {
            ExternalAriatusModule module = loadedModule.instance();
            boolean unregistered = core.moduleManager().unregister(moduleId);
            if (!unregistered) {
                return false;
            }
            core.taskManager().cancelAll(module);
            core.listenerManager().unregisterAll(module);
            core.commandManager().unregisterAll(module);
            core.moduleConfigManager().unload(module);

            loadedModules.remove(moduleId);
            loadedModule.classLoader().close();
            failedModules.remove(moduleId);

            core.loggerService().info("Módulo externo descargado: " + moduleId);
            return true;

        } catch (Exception exception) {
            failedModules.put(moduleId, exception.getMessage());
            core.loggerService().error("Error descargando módulo " + moduleId + ": " + exception.getMessage());
            return false;
        }
    }

    public boolean reloadModule(String id) {
        String moduleId = id.toLowerCase();

        LoadedModule oldModule = loadedModules.get(moduleId);

        if (oldModule == null) {
            failedModules.put(moduleId, "No está cargado");
            return false;
        }

        ModuleDescriptor descriptor = oldModule.descriptor();

        boolean wasEnabled = oldModule.instance().status() == ModuleStatus.ENABLED;

        boolean unloaded = unloadModule(moduleId);

        if (!unloaded) {
            return false;
        }

        boolean loaded = loadModule(descriptor);

        if (!loaded) {
            return false;
        }

        if (wasEnabled) {
            core.moduleManager().enable(moduleId);
        }

        core.loggerService().info("Módulo externo recargado: " + moduleId);
        return true;
    }

    public void unloadAll() {
        List<String> ids = new ArrayList<>(loadedModules.keySet());
        Collections.reverse(ids);
        for (String id : ids) {
            boolean unloaded = unloadModule(id);
            if (!unloaded) {
                core.loggerService().warn("No se pudo descargar correctamente el módulo: " + id);
            }
        }
        loadedModules.clear();
    }

    public List<ModuleDescriptor> discoveredModules() {
        return List.copyOf(discoveredModules);
    }

    public Map<String, LoadedModule> loadedModules() {
        return Collections.unmodifiableMap(loadedModules);
    }

    public Map<String, String> failedModules() {
        return Collections.unmodifiableMap(failedModules);
    }
}