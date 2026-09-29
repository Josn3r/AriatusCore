package net.ariatus.project.module.config;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ModuleConfigManager {

    private final AriatusCore core;
    private final ConcurrentMap<String, ModuleConfig> configs = new ConcurrentHashMap<>();

    public ModuleConfigManager(AriatusCore core) {
        this.core = Objects.requireNonNull(core, "core");
    }

    public ModuleConfig load(AriatusModule module) {
        return load(module, "config.yml");
    }

    public ModuleConfig load(AriatusModule module, String fileName) {
        Objects.requireNonNull(module, "module");

        String normalizedName = normalizeFileName(fileName);
        String key = key(module, normalizedName);

        ModuleConfig loaded = configs.get(key);

        if (loaded != null) {
            return loaded;
        }

        try {
            Path file = resolve(module, normalizedName);

            Files.createDirectories(file.getParent());

            if (Files.notExists(file)) {
                copyDefaultResource(module, normalizedName, file);

                if (Files.notExists(file)) {
                    Files.createFile(file);
                }
            }

            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file.toFile());
            ModuleConfig moduleConfig = new ModuleConfig(file.toFile(), configuration);

            configs.put(key, moduleConfig);

            module.logger().debug("Configuración cargada: " + normalizedName);

            return moduleConfig;

        } catch (Exception exception) {
            module.logger().error("No se pudo cargar " + normalizedName + ".", exception);
            throw new IllegalStateException("No se pudo cargar " + normalizedName + " del módulo " + module.id() + ".", exception);
        }
    }

    public ModuleConfig reload(AriatusModule module) {
        return reload(module, "config.yml");
    }

    public ModuleConfig reload(AriatusModule module, String fileName) {
        String normalizedName = normalizeFileName(fileName);

        configs.remove(key(module, normalizedName));

        return load(module, normalizedName);
    }

    public ModuleConfig get(AriatusModule module) {
        return get(module, "config.yml");
    }

    public ModuleConfig get(AriatusModule module, String fileName) {
        return configs.get(key(module, normalizeFileName(fileName)));
    }

    public boolean save(AriatusModule module) {
        return save(module, "config.yml");
    }

    public boolean save(AriatusModule module, String fileName) {
        String normalizedName = normalizeFileName(fileName);
        ModuleConfig config = get(module, normalizedName);

        if (config == null) {
            module.logger().warn("No se puede guardar una configuración no cargada: " + normalizedName);
            return false;
        }

        try {
            config.configuration().save(config.file());
            return true;
        } catch (IOException exception) {
            module.logger().error("No se pudo guardar " + normalizedName + ".", exception);
            return false;
        }
    }

    public int saveAll(AriatusModule module) {
        String prefix = module.id().toLowerCase(Locale.ROOT) + ":";
        int saved = 0;

        for (Map.Entry<String, ModuleConfig> entry : configs.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }

            try {
                entry.getValue().configuration().save(entry.getValue().file());
                saved++;
            } catch (IOException exception) {
                module.logger().error("No se pudo guardar " + entry.getValue().file().getName() + ".", exception);
            }
        }

        return saved;
    }

    public boolean exists(AriatusModule module, String fileName) {
        return Files.exists(resolve(module, normalizeFileName(fileName)));
    }

    public File file(AriatusModule module, String fileName) {
        return resolve(module, normalizeFileName(fileName)).toFile();
    }

    public void saveDefaultResource(AriatusModule module, String fileName) {
        String normalizedName = normalizeFileName(fileName);
        Path target = resolve(module, normalizedName);

        try {
            Files.createDirectories(target.getParent());

            if (Files.exists(target)) {
                return;
            }

            if (!copyDefaultResource(module, normalizedName, target)) {
                module.logger().warn("El recurso " + normalizedName + " no existe dentro del JAR del módulo.");
            }
        } catch (Exception exception) {
            module.logger().error("No se pudo copiar el recurso " + normalizedName + ".", exception);
        }
    }

    public int loaded(AriatusModule module) {
        String prefix = module.id().toLowerCase(Locale.ROOT) + ":";

        return (int) configs.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .count();
    }

    public void unload(AriatusModule module) {
        String prefix = module.id().toLowerCase(Locale.ROOT) + ":";

        configs.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void unloadAll() {
        configs.clear();
    }

    private boolean copyDefaultResource(AriatusModule module, String resourceName, Path target) throws IOException {
        File moduleFile = module.descriptor().file();

        try (JarFile jarFile = new JarFile(moduleFile)) {
            JarEntry entry = jarFile.getJarEntry(resourceName);

            if (entry == null || entry.isDirectory()) {
                return false;
            }

            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        }
    }

    private Path resolve(AriatusModule module, String fileName) {
        Path directory = core.moduleDataManager().path(module);
        Path target = directory.resolve(fileName).normalize();

        if (!target.startsWith(directory)) {
            throw new IllegalArgumentException("Ruta de configuración inválida: " + fileName);
        }

        return target;
    }

    private String normalizeFileName(String fileName) {
        String value = Objects.requireNonNull(fileName, "fileName")
                .trim()
                .replace('\\', '/');

        if (value.isEmpty()) {
            throw new IllegalArgumentException("El nombre del archivo no puede estar vacío.");
        }

        if (value.startsWith("/") || value.contains("../") || value.equals("..")) {
            throw new IllegalArgumentException("Ruta de configuración inválida: " + fileName);
        }

        if (!value.toLowerCase(Locale.ROOT).endsWith(".yml")) {
            throw new IllegalArgumentException("Las configuraciones de módulos deben ser archivos .yml: " + fileName);
        }

        return value;
    }

    private String key(AriatusModule module, String fileName) {
        return module.id().toLowerCase(Locale.ROOT) + ":" + fileName.toLowerCase(Locale.ROOT);
    }
}