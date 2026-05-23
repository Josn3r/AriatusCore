package net.ariatus.project.profile;

public class ProfilePreferences {

    private boolean scoreboardEnabled;
    private boolean tablistEnabled;
    private boolean privateMessagesEnabled;
    private boolean tradeRequestsEnabled;
    private boolean duelRequestsEnabled;
    private boolean musicEnabled;
    private boolean particlesEnabled;
    private boolean cinematicsEnabled;

    public ProfilePreferences(
            boolean scoreboardEnabled,
            boolean tablistEnabled,
            boolean privateMessagesEnabled,
            boolean tradeRequestsEnabled,
            boolean duelRequestsEnabled,
            boolean musicEnabled,
            boolean particlesEnabled,
            boolean cinematicsEnabled
    ) {
        this.scoreboardEnabled = scoreboardEnabled;
        this.tablistEnabled = tablistEnabled;
        this.privateMessagesEnabled = privateMessagesEnabled;
        this.tradeRequestsEnabled = tradeRequestsEnabled;
        this.duelRequestsEnabled = duelRequestsEnabled;
        this.musicEnabled = musicEnabled;
        this.particlesEnabled = particlesEnabled;
        this.cinematicsEnabled = cinematicsEnabled;
    }

    public static ProfilePreferences defaults() {
        return new ProfilePreferences(
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true
        );
    }

    public boolean scoreboardEnabled() {
        return scoreboardEnabled;
    }

    public boolean tablistEnabled() {
        return tablistEnabled;
    }

    public boolean privateMessagesEnabled() {
        return privateMessagesEnabled;
    }

    public boolean tradeRequestsEnabled() {
        return tradeRequestsEnabled;
    }

    public boolean duelRequestsEnabled() {
        return duelRequestsEnabled;
    }

    public boolean musicEnabled() {
        return musicEnabled;
    }

    public boolean particlesEnabled() {
        return particlesEnabled;
    }

    public boolean cinematicsEnabled() {
        return cinematicsEnabled;
    }

    public void scoreboardEnabled(boolean scoreboardEnabled) {
        this.scoreboardEnabled = scoreboardEnabled;
    }

    public void tablistEnabled(boolean tablistEnabled) {
        this.tablistEnabled = tablistEnabled;
    }

    public void privateMessagesEnabled(boolean privateMessagesEnabled) {
        this.privateMessagesEnabled = privateMessagesEnabled;
    }

    public void tradeRequestsEnabled(boolean tradeRequestsEnabled) {
        this.tradeRequestsEnabled = tradeRequestsEnabled;
    }

    public void duelRequestsEnabled(boolean duelRequestsEnabled) {
        this.duelRequestsEnabled = duelRequestsEnabled;
    }

    public void musicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;
    }

    public void particlesEnabled(boolean particlesEnabled) {
        this.particlesEnabled = particlesEnabled;
    }

    public void cinematicsEnabled(boolean cinematicsEnabled) {
        this.cinematicsEnabled = cinematicsEnabled;
    }
}