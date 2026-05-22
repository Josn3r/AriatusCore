package net.ariatus.project.module.config;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ModuleConfigManager {

    private final AriatusCore core;
    private final Map<String, ModuleConfig> configs = new HashMap<>();

    public ModuleConfigManager(AriatusCore core) {
        this.core = core;
    }

    public ModuleConfig load(AriatusModule module) {
        return load(module, "config.yml");
    }

    public ModuleConfig load(AriatusModule module, String fileName) {
        String key = key(module, fileName);

        File folder = core.moduleDataManager().folder(module);
        File file = new File(folder, fileName);

        try {
            if (!file.exists()) {
                saveDefaultResource(module, fileName);

                if (!file.exists()) {
                    file.createNewFile();
                    core.loggerService().info(module, fileName + " creado vacío.");
                }
            }

            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

            ModuleConfig moduleConfig = new ModuleConfig(file, configuration);
            configs.put(key, moduleConfig);

            return moduleConfig;

        } catch (Exception exception) {
            core.loggerService().error(module, "Error cargando " + fileName + ": " + exception.getMessage());
            return null;
        }
    }

    public ModuleConfig reload(AriatusModule module) {
        return reload(module, "config.yml");
    }

    public ModuleConfig reload(AriatusModule module, String fileName) {
        configs.remove(key(module, fileName));
        return load(module, fileName);
    }

    public ModuleConfig get(AriatusModule module) {
        return get(module, "config.yml");
    }

    public ModuleConfig get(AriatusModule module, String fileName) {
        return configs.get(key(module, fileName));
    }

    public void unload(AriatusModule module) {
        String prefix = module.id().toLowerCase() + ":";

        configs.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void unloadAll() {
        configs.clear();
    }

    public void saveDefaultResource(AriatusModule module, String resourceName) {
        String moduleId = module.id().toLowerCase();

        var loadedModule = core.moduleLoader().loadedModules().get(moduleId);

        if (loadedModule == null) {
            core.loggerService().warn(module, "No se pudo encontrar el módulo cargado para copiar: " + resourceName);
            return;
        }

        File folder = core.moduleDataManager().folder(module);
        File targetFile = new File(folder, resourceName);

        if (targetFile.exists()) {
            return;
        }

        try {
            File parent = targetFile.getParentFile();

            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (var jarFile = new java.util.jar.JarFile(loadedModule.descriptor().file())) {
                var entry = jarFile.getJarEntry(resourceName);

                if (entry == null) {
                    core.loggerService().warn(module, "Recurso no encontrado dentro del JAR del módulo: " + resourceName);
                    return;
                }

                try (var inputStream = jarFile.getInputStream(entry)) {
                    java.nio.file.Files.copy(inputStream, targetFile.toPath());
                }
            }

            core.loggerService().info(module, "Archivo creado desde resources del módulo: " + resourceName);

        } catch (Exception exception) {
            core.loggerService().error(module, "Error copiando " + resourceName + ": " + exception.getMessage());
        }
    }

    private String key(AriatusModule module, String fileName) {
        return module.id().toLowerCase() + ":" + fileName.toLowerCase();
    }
}