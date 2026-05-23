package net.ariatus.project.profile;

import net.ariatus.project.api.profile.ProfileView;

import java.time.Instant;
import java.util.UUID;

public class Profile implements ProfileView {

    private final UUID uuid;

    private String name;

    private int level;
    private long experience;
    private long totalExperience;

    private int reputation;
    private long playtimeSeconds;

    private Instant firstLogin;
    private Instant lastLogin;

    private final ProfileStats stats;
    private final ProfilePreferences preferences;
    private final ProfileMetadata metadata;
    private final ProfileCooldowns cooldowns;

    public Profile(
            UUID uuid,
            String name,
            int level,
            long experience,
            long totalExperience,
            int reputation,
            long playtimeSeconds,
            Instant firstLogin,
            Instant lastLogin,
            ProfileStats stats,
            ProfilePreferences preferences,
            ProfileMetadata metadata,
            ProfileCooldowns cooldowns
    ) {
        this.uuid = uuid;
        this.name = name;
        this.level = level;
        this.experience = experience;
        this.totalExperience = totalExperience;
        this.reputation = reputation;
        this.playtimeSeconds = playtimeSeconds;
        this.firstLogin = firstLogin;
        this.lastLogin = lastLogin;
        this.stats = stats;
        this.preferences = preferences;
        this.metadata = metadata;
        this.cooldowns = cooldowns;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public int level() {
        return level;
    }

    public long experience() {
        return experience;
    }

    public long totalExperience() {
        return totalExperience;
    }

    public int reputation() {
        return reputation;
    }

    public long playtimeSeconds() {
        return playtimeSeconds;
    }

    public Instant firstLogin() {
        return firstLogin;
    }

    public Instant lastLogin() {
        return lastLogin;
    }

    public ProfileStats stats() {
        return stats;
    }

    public ProfilePreferences preferences() {
        return preferences;
    }

    public ProfileMetadata metadata() {
        return metadata;
    }

    public ProfileCooldowns cooldowns() {
        return cooldowns;
    }

    public void name(String name) {
        this.name = name;
    }

    public void level(int level) {
        this.level = Math.max(1, level);
    }

    public void experience(long experience) {
        this.experience = Math.max(0, experience);
    }

    public void totalExperience(long totalExperience) {
        this.totalExperience = Math.max(0, totalExperience);
    }

    public void addExperience(long amount) {
        if (amount <= 0) {
            return;
        }

        this.experience += amount;
        this.totalExperience += amount;
    }

    public void reputation(int reputation) {
        this.reputation = reputation;
    }

    public void addReputation(int amount) {
        this.reputation += amount;
    }

    public void removeReputation(int amount) {
        this.reputation -= amount;
    }

    public void playtimeSeconds(long playtimeSeconds) {
        this.playtimeSeconds = Math.max(0, playtimeSeconds);
    }

    public void addPlaytimeSeconds(long seconds) {
        if (seconds <= 0) {
            return;
        }

        this.playtimeSeconds += seconds;
    }

    public void firstLogin(Instant firstLogin) {
        this.firstLogin = firstLogin;
    }

    public void lastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public long kills() {
        return stats.kills();
    }

    @Override
    public long deaths() {
        return stats.deaths();
    }

    @Override
    public long blocksBroken() {
        return stats.blocksBroken();
    }

    @Override
    public long blocksPlaced() {
        return stats.blocksPlaced();
    }

}