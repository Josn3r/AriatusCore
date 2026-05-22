package net.ariatus.project.module.loader;

import net.ariatus.project.module.ExternalAriatusModule;

public record LoadedModule(
        ModuleDescriptor descriptor,
        ExternalAriatusModule instance,
        AriatusModuleClassLoader classLoader
) {
}