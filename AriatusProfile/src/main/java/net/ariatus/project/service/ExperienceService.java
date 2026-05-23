package net.ariatus.project.service;

import net.ariatus.project.AriatusProfile;
import net.ariatus.project.api.profile.ExperienceProvider;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.profile.Profile;
import org.bukkit.entity.Player;

public class ExperienceService implements ExperienceProvider {

    private final AriatusProfile module;

    public ExperienceService(AriatusProfile module) {
        this.module = module;
    }

    public void addExperience(Player player, Profile profile, long amount) {
        if (amount <= 0) {
            return;
        }

        int maxLevel = maxLevel();

        if (profile.level() >= maxLevel) {
            profile.totalExperience(profile.totalExperience() + amount);
            MessageService.send(player, message("experience.messages.experience-added")
                    .replace("%amount%", String.valueOf(amount)));
            return;
        }

        profile.addExperience(amount);

        MessageService.send(player, message("experience.messages.experience-added")
                .replace("%amount%", String.valueOf(amount)));

        processLevelUps(player, profile);
    }

    public void setExperience(Player player, Profile profile, long experience) {
        profile.experience(Math.max(0, experience));
        processLevelUps(player, profile);
    }

    public void setLevel(Player player, Profile profile, int level) {
        int finalLevel = Math.max(1, Math.min(level, maxLevel()));

        profile.level(finalLevel);
        profile.experience(0);

        MessageService.send(player, "&aTu nivel fue establecido en &e" + finalLevel + "&a.");
    }

    public long requiredExperience(int level) {
        int base = module.configInt("config.yml", "experience.formula.base", 100);
        double multiplier = module.config("config.yml").getDouble("experience.formula.multiplier", 1.35);

        return Math.max(1, Math.round(base * Math.pow(level, multiplier)));
    }

    public int maxLevel() {
        return module.configInt("config.yml", "experience.formula.max-level", 300);
    }

    private void processLevelUps(Player player, Profile profile) {
        int maxLevel = maxLevel();

        while (profile.level() < maxLevel) {
            long required = requiredExperience(profile.level());

            if (profile.experience() < required) {
                break;
            }

            profile.experience(profile.experience() - required);
            profile.level(profile.level() + 1);

            MessageService.send(player, message("experience.messages.level-up")
                    .replace("%level%", String.valueOf(profile.level())));
        }

        if (profile.level() >= maxLevel) {
            profile.level(maxLevel);
        }
    }

    private String message(String path) {
        return module.configString("config.yml", path, path);
    }
}