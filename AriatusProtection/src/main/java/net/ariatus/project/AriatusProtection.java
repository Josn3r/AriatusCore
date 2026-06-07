package net.ariatus.project;

import net.ariatus.project.commands.ProtectionCommand;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.protection.ProtectionManager;
import net.ariatus.project.protection.WorldProtectionListener;

public class AriatusProtection extends ExternalAriatusModule {

    private ModuleStatus status = ModuleStatus.DISABLED;
    private ProtectionManager protectionManager;

    @Override
    public String id() {
        return "protection";
    }

    @Override
    public String name() {
        return "AriatusProtection";
    }

    @Override
    public void enable() {
        status = ModuleStatus.ENABLING;

        loadConfig("config.yml");

        this.protectionManager = new ProtectionManager(this);
        this.protectionManager.load();

        listeners().register(this, new WorldProtectionListener(protectionManager));
        commands().register(this, new ProtectionCommand(protectionManager));

        status = ModuleStatus.ENABLED;
        logger().info(this, "AriatusProtection activado correctamente.");
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLING;

        status = ModuleStatus.DISABLED;
        logger().info(this, "AriatusProtection desactivado correctamente.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }
}