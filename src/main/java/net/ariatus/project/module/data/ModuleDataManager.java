package net.ariatus.project.module.data;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;

import java.io.File;

public class ModuleDataManager {

    private final AriatusCore core;
    private final File modulesDataFolder;

    public ModuleDataManager(AriatusCore core) {
        this.core = core;
        this.modulesDataFolder = new File(core.getDataFolder(), "modules-data");
    }

    public void load() {
        if (!modulesDataFolder.exists() && modulesDataFolder.mkdirs()) {
            core.loggerService().info("Carpeta modules-data creada.");
        }
    }

    public File folder(AriatusModule module) {
        File folder = new File(modulesDataFolder, module.id().toLowerCase());

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }
}