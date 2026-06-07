package net.ariatus.project;

import net.ariatus.project.api.profile.ExperienceProvider;
import net.ariatus.project.api.profile.ProfileService;
import net.ariatus.project.commands.ProfileAdminCommand;
import net.ariatus.project.commands.ProfileCommand;
import net.ariatus.project.listener.ProfileJoinListener;
import net.ariatus.project.listener.ProfileQuitListener;
import net.ariatus.project.listener.ProfileStatsListener;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.profile.ProfileManager;
import net.ariatus.project.service.ExperienceService;
import net.ariatus.project.storage.MariaDBProfileRepository;
import net.ariatus.project.storage.ProfileRepository;
import org.bukkit.Bukkit;

public class AriatusProfile extends ExternalAriatusModule {

    private ModuleStatus status = ModuleStatus.DISABLED;

    private ProfileRepository repository;
    private ProfileManager profileManager;
    private ExperienceService experienceService;

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
        status = ModuleStatus.ENABLING;

        loadConfig("config.yml");

        this.repository = new MariaDBProfileRepository(this);
        this.profileManager = new ProfileManager(this, repository);
        this.experienceService = new ExperienceService(this);

        services().register(ProfileService.class, profileManager);
        services().register(ExperienceProvider.class, experienceService);

        listeners().register(this, new ProfileJoinListener(profileManager));
        listeners().register(this, new ProfileQuitListener(profileManager));
        listeners().register(this, new ProfileStatsListener(profileManager));

        Bukkit.getOnlinePlayers().forEach(profileManager::loadProfile);

        if (configBoolean("config.yml", "profile.autosave.enabled", true)) {
            long interval = configInt("config.yml", "profile.autosave.interval-ticks", 6000);
            tasks().runRepeating(this, profileManager::saveAll, interval, interval);
        }


        commands().register(this, new ProfileCommand(profileManager));
        commands().register(this, new ProfileAdminCommand(profileManager, experienceService));


        status = ModuleStatus.ENABLED;
        logger().info(this, "AriatusProfile activado correctamente.");
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLING;

        if (profileManager != null) {
            profileManager.saveAllNow();
            profileManager.clear();
        }

        status = ModuleStatus.DISABLED;
        logger().info(this, "AriatusProfile desactivado correctamente.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }
}