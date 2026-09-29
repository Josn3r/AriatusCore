package net.ariatus.project.module.data;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ModuleDataManager {

    private final AriatusCore core;
    private final Path modulesDataDirectory;

    public ModuleDataManager(AriatusCore core) {
        this.core = Objects.requireNonNull(core, "core");
        this.modulesDataDirectory = core.getDataFolder().toPath().resolve("modules-data");
    }

    public void load() {
        try {
            Files.createDirectories(modulesDataDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la carpeta modules-data.", exception);
        }
    }

    public Path path(AriatusModule module) {
        Objects.requireNonNull(module, "module");

        Path moduleDirectory = modulesDataDirectory.resolve(module.id()).normalize();

        if (!moduleDirectory.startsWith(modulesDataDirectory)) {
            throw new IllegalStateException("Ruta de datos inválida para el módulo " + module.id() + ".");
        }

        try {
            Files.createDirectories(moduleDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la carpeta de datos del módulo " + module.id() + ".", exception);
        }

        return moduleDirectory;
    }

    public File folder(AriatusModule module) {
        return path(module).toFile();
    }

    public Path root() {
        return modulesDataDirectory;
    }
}