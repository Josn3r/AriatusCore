package net.ariatus.project.api.profile;

public interface ExperienceProvider {

    long requiredExperience(int level);

    int maxLevel();
}