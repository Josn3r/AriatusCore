package net.ariatus.project;

import net.ariatus.project.api.economy.EconomyService;
import net.ariatus.project.commands.EconomyAdminCommand;
import net.ariatus.project.commands.MoneyCommand;
import net.ariatus.project.commands.PayCommand;
import net.ariatus.project.economy.EconomyManager;
import net.ariatus.project.economy.EconomyRepository;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.storage.MariaDBEconomyRepository;

public class AriatusEconomy extends ExternalAriatusModule {

    private ModuleStatus status = ModuleStatus.DISABLED;

    private EconomyRepository repository;
    private EconomyManager economyManager;

    @Override
    public String id() {
        return "economy";
    }

    @Override
    public String name() {
        return "AriatusEconomy";
    }

    @Override
    public void enable() {
        status = ModuleStatus.ENABLING;

        loadConfig("config.yml");

        this.repository = new MariaDBEconomyRepository(this);
        this.economyManager = new EconomyManager(this, repository);

        services().register(EconomyService.class, economyManager);

        commands().register(this, new MoneyCommand(economyManager));
        commands().register(this, new PayCommand(economyManager));
        commands().register(this, new EconomyAdminCommand(economyManager));

        status = ModuleStatus.ENABLED;
        logger().info(this, "AriatusEconomy activado correctamente.");
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLING;

        status = ModuleStatus.DISABLED;
        logger().info(this, "AriatusEconomy desactivado correctamente.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }
}