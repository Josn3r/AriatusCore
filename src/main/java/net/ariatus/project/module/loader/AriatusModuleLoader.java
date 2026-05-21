package net.ariatus.project.module.loader;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.ExternalAriatusModule;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

public class AriatusModuleLoader {

    private final AriatusCore core;
    private final File modulesFolder;
    private final List<ModuleDescriptor> discoveredModules = new ArrayList<>();
    private final Map<String, ExternalAriatusModule> loadedModules = new HashMap<>();
    private final Map<String, AriatusModuleClassLoader> classLoaders = new HashMap<>();

    public AriatusModuleLoader(AriatusCore core) {
        this.core = core;
        this.modulesFolder = new File(core.getDataFolder(), "modules");
    }

    public void loadFolder() {
        if (!modulesFolder.exists()) {
            modulesFolder.mkdirs();
            core.loggerService().info("Carpeta de módulos creada: " + modulesFolder.getPath());
        }
    }

    public void discoverModules() {
        discoveredModules.clear();
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

                if (id == null || name == null || main == null) {
                    core.loggerService().warn("Módulo inválido en " + file.getName() + ": faltan id, name o main.");
                    return;
                }

                ModuleDescriptor descriptor = new ModuleDescriptor(
                        id.toLowerCase(),
                        name,
                        main,
                        version,
                        dependencies,
                        file
                );

                discoveredModules.add(descriptor);

                core.loggerService().info("Módulo externo detectado: " + name + " v" + version + " (" + id + ")");
            }

        } catch (Exception exception) {
            core.loggerService().error("Error leyendo módulo " + file.getName() + ": " + exception.getMessage());
        }
    }

    public void loadModules() {
        for (ModuleDescriptor descriptor : discoveredModules) {
            loadModule(descriptor);
        }
    }

    private void loadModule(ModuleDescriptor descriptor) {
        try {
            URL url = descriptor.file().toURI().toURL();
            AriatusModuleClassLoader classLoader =
                    new AriatusModuleClassLoader(
                            new URL[]{url},
                            core.getClass().getClassLoader()
                    );
            Class<?> clazz = classLoader.loadClass(descriptor.main());
            if (!ExternalAriatusModule.class.isAssignableFrom(clazz)) {
                core.loggerService().error("El módulo " + descriptor.id()
                        + " no extiende ExternalAriatusModule.");
                return;
            }
            ExternalAriatusModule module =
                    (ExternalAriatusModule) clazz.getDeclaredConstructor().newInstance();
            module.initialize(core);
            loadedModules.put(descriptor.id(), module);
            classLoaders.put(descriptor.id(), classLoader);
            core.moduleManager().register(module);
            core.loggerService().info("Módulo externo cargado: " + descriptor.name());
        } catch (Exception exception) {
            core.loggerService().error("No se pudo cargar módulo "
                    + descriptor.id() + ": " + exception.getMessage());
        }
    }

    public List<ModuleDescriptor> discoveredModules() {
        return List.copyOf(discoveredModules);
    }
    public Map<String, ExternalAriatusModule> loadedModules() {
        return loadedModules;
    }
}