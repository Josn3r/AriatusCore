package net.ariatus.project.module;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.loader.AriatusModuleClassLoader;

import java.util.Objects;

public final class ModuleContainer {

    private final ModuleDescriptor descriptor;
    private final AriatusModule module;
    private final AriatusModuleClassLoader classLoader;
    private final ModuleContext context;

    private volatile ModuleStatus status = ModuleStatus.DISABLED;

    public ModuleContainer(AriatusCore core, ModuleDescriptor descriptor, AriatusModule module, AriatusModuleClassLoader classLoader) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.module = Objects.requireNonNull(module, "module");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
        this.context = new ModuleContext(Objects.requireNonNull(core, "core"), this);

        this.module.initialize(context);
    }

    public ModuleDescriptor descriptor() {
        return descriptor;
    }

    public AriatusModule module() {
        return module;
    }

    public AriatusModuleClassLoader classLoader() {
        return classLoader;
    }

    public ModuleContext context() {
        return context;
    }

    public ModuleStatus status() {
        return status;
    }

    public boolean isEnabled() {
        return status == ModuleStatus.ENABLED;
    }

    void status(ModuleStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }
}