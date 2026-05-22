package net.ariatus.project;

import net.ariatus.project.commands.ProfileCommand;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;

public final class AriatusProfile extends ExternalAriatusModule {

    private ModuleStatus status = ModuleStatus.DISABLED;

    @Override
    public String id() {
        return "profile";
    }

    @Override
    public String name() {
        return "AriatusProfile";
    }

    @Override
    public void enable() {
        status = ModuleStatus.ENABLED;
        core().loggerService().info(this, "AriatusProfile activado correctamente.");
        commands().register(this, new ProfileCommand());
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLED;
        core().loggerService().info(this, "AriatusProfile desactivado correctamente.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }

}