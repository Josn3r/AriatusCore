package net.ariatus.project.profile;

public class ProfileStats {

    private long kills;
    private long deaths;
    private long playerKills;
    private long mobKills;

    private long blocksBroken;
    private long blocksPlaced;

    private long questsCompleted;
    private long dungeonsCompleted;
    private long bossesKilled;

    public ProfileStats(
            long kills,
            long deaths,
            long playerKills,
            long mobKills,
            long blocksBroken,
            long blocksPlaced,
            long questsCompleted,
            long dungeonsCompleted,
            long bossesKilled
    ) {
        this.kills = kills;
        this.deaths = deaths;
        this.playerKills = playerKills;
        this.mobKills = mobKills;
        this.blocksBroken = blocksBroken;
        this.blocksPlaced = blocksPlaced;
        this.questsCompleted = questsCompleted;
        this.dungeonsCompleted = dungeonsCompleted;
        this.bossesKilled = bossesKilled;
    }

    public static ProfileStats defaults() {
        return new ProfileStats(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public long kills() {
        return kills;
    }

    public long deaths() {
        return deaths;
    }

    public long playerKills() {
        return playerKills;
    }

    public long mobKills() {
        return mobKills;
    }

    public long blocksBroken() {
        return blocksBroken;
    }

    public long blocksPlaced() {
        return blocksPlaced;
    }

    public long questsCompleted() {
        return questsCompleted;
    }

    public long dungeonsCompleted() {
        return dungeonsCompleted;
    }

    public long bossesKilled() {
        return bossesKilled;
    }

    public void addKill() {
        this.kills++;
    }

    public void addDeath() {
        this.deaths++;
    }

    public void addPlayerKill() {
        this.playerKills++;
        this.kills++;
    }

    public void addMobKill() {
        this.mobKills++;
        this.kills++;
    }

    public void addBlockBroken() {
        this.blocksBroken++;
    }

    public void addBlockPlaced() {
        this.blocksPlaced++;
    }

    public void addQuestCompleted() {
        this.questsCompleted++;
    }

    public void addDungeonCompleted() {
        this.dungeonsCompleted++;
    }

    public void addBossKilled() {
        this.bossesKilled++;
    }
}